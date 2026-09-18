package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** A deliberately small HTTP/1.1 GET parser: one request, one response, then close. */
public final class Handler implements Runnable {
    private final Socket socket;
    private final long deadline;
    private int headerBytes;
    private boolean headRequest;
    public Handler(Socket socket) { this(socket, 5000); }
    public Handler(Socket socket, int timeoutMillis) {
        this.socket = socket;
        this.deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
    }
    @Override public void run() {
        try (socket) {
            HttpServletResponse response;
            try { response = dispatch(readRequest()); }
            catch (BadRequest e) { response = error(e.status, e.getMessage()); }
            catch (SocketTimeoutException e) { response = error(408, "Request header deadline exceeded"); }
            catch (RuntimeException e) { response = error(500, "Controller failed"); }
            writeResponse(socket.getOutputStream(), response, headRequest);
        } catch (IOException e) {
            // A disconnected client has no response channel; never retry a partial response.
        }
    }
    private HttpServletRequest readRequest() throws IOException {
        InputStream input = socket.getInputStream();
        String line = readLine(input, 2048);
        String[] requestLine = line.split(" ", -1);
        if (requestLine.length != 3 || !"HTTP/1.1".equals(requestLine[2]))
            throw new BadRequest(400, "Expected METHOD /target HTTP/1.1");
        headRequest = "HEAD".equals(requestLine[0]);
        Map<String, String> headers = new HashMap<>();
        while (!(line = readLine(input, 8192)).isEmpty()) {
            int colon = line.indexOf(':');
            if (colon < 1 || !line.substring(0, colon).matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+"))
                throw new BadRequest(400, "Malformed header");
            String name = line.substring(0, colon).toLowerCase(Locale.ROOT);
            if (headers.putIfAbsent(name, line.substring(colon + 1).trim()) != null)
                throw new BadRequest(400, "Duplicate headers are unsupported");
            if (headers.size() > 32) throw new BadRequest(431, "Too many headers");
        }
        if (headers.getOrDefault("host", "").isEmpty()) throw new BadRequest(400, "Host is required");
        if (headers.containsKey("transfer-encoding")) throw new BadRequest(501, "Request bodies are unsupported");
        if (headers.containsKey("content-length") && !"0".equals(headers.get("content-length")))
            throw new BadRequest(400, "This server accepts no request body");
        if (!"GET".equals(requestLine[0])) throw new BadRequest(405, "Only GET is supported");
        URI target;
        try { target = URI.create(requestLine[1]); }
        catch (IllegalArgumentException e) { throw new BadRequest(400, "Malformed target"); }
        if (!requestLine[1].startsWith("/") || target.isAbsolute() || target.getRawAuthority() != null
                || target.getRawFragment() != null || target.getRawPath() == null)
            throw new BadRequest(400, "Expected an origin-form target");
        String path = target.getRawPath();
        if (!path.matches("/[A-Za-z0-9_-]*")) throw new BadRequest(404, "Unknown route");
        Map<String, String> parameters = new HashMap<>();
        String query = target.getRawQuery();
        if (query != null && !query.isEmpty()) {
            for (String part : query.split("&", -1)) {
                int equal = part.indexOf('=');
                if (equal < 1) throw new BadRequest(400, "Expected name=value");
                String name = decode(part.substring(0, equal));
                String value = decode(part.substring(equal + 1));
                if (name.isBlank() || parameters.putIfAbsent(name, value) != null)
                    throw new BadRequest(400, "Empty or duplicate parameter");
            }
        }
        return new HttpServletRequest(path.substring(1), parameters);
    }
    private String readLine(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int previous = -1;
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new SocketTimeoutException("Header deadline");
            socket.setSoTimeout((int) Math.max(1L, Math.min(Integer.MAX_VALUE, (remaining + 999_999) / 1_000_000)));
            int value = input.read();
            if (value == -1) throw new BadRequest(400, "Incomplete request");
            if (++headerBytes > 8192) throw new BadRequest(431, "Headers exceed 8192 bytes");
            if (value == '\n') {
                byte[] content = bytes.toByteArray();
                if (content.length == 0 || content[content.length - 1] != '\r')
                    throw new BadRequest(400, "CRLF required");
                return new String(content, 0, content.length - 1, StandardCharsets.US_ASCII);
            }
            if (value > 126 || (value < 32 && value != '\r' && value != '\t')
                    || previous == '\r')
                throw new BadRequest(400, "Invalid header character");
            bytes.write(value);
            previous = value;
            if (bytes.size() > limit) throw new BadRequest(431, "Line too long");
        }
    }
    private static String decode(String value) throws BadRequest {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '%') {
                    if (i + 2 >= value.length()) throw new IllegalArgumentException("Incomplete escape");
                    int high = Character.digit(value.charAt(++i), 16);
                    int low = Character.digit(value.charAt(++i), 16);
                    if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid escape");
                    bytes.write(high * 16 + low);
                } else {
                    if (c > 127) throw new IllegalArgumentException("Non-ASCII request target");
                    bytes.write(c == '+' ? ' ' : c);
                }
            }
            // REPORT distinguishes malformed bytes from a legitimately encoded U+FFFD.
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            if (decoded.chars().anyMatch(c -> c < 32 || c == 127))
                throw new IllegalArgumentException("Invalid text");
            return decoded;
        } catch (CharacterCodingException | IllegalArgumentException e) {
            throw new BadRequest(400, "Malformed query encoding");
        }
    }
    private static HttpServletResponse dispatch(HttpServletRequest request) {
        HttpServlet controller = MyServerReader.getController(request.getRequestName());
        if (controller == null) return error(404, "Unknown route");
        HttpServletResponse response = new HttpServletResponse();
        controller.service(request, response);
        return response;
    }
    private static HttpServletResponse error(int status, String message) {
        HttpServletResponse response = new HttpServletResponse();
        response.setStatus(status);
        response.setContent(message + "\n");
        return response;
    }
    private static void writeResponse(OutputStream output, HttpServletResponse response, boolean headRequest) throws IOException {
        byte[] body = response.getContent().getBytes(StandardCharsets.UTF_8);
        String reason = switch (response.getStatus()) {
            case 200 -> "OK"; case 400 -> "Bad Request"; case 404 -> "Not Found";
            case 405 -> "Method Not Allowed"; case 408 -> "Request Timeout";
            case 431 -> "Request Header Fields Too Large"; case 501 -> "Not Implemented";
            default -> "Internal Server Error";
        };
        String headers = "HTTP/1.1 " + response.getStatus() + " " + reason
                + "\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\nX-Content-Type-Options: nosniff\r\nCache-Control: no-store\r\n"
                + (response.getStatus() == 405 ? "Allow: GET\r\n" : "") + "\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        if (!headRequest) output.write(body);
        output.flush();
    }
    private static final class BadRequest extends IOException {
        private static final long serialVersionUID = 1L;
        private final int status;
        private BadRequest(int status, String message) { super(message); this.status = status; }
    }
}
