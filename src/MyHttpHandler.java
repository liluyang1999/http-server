import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MyHttpHandler implements HttpHandler {
    public static final int MAX_BODY_BYTES = 8192;
    private final HandlerUtil replies = new HandlerUtil();
    private record Calculation(String operation, double left, double right) {}

    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getRawPath();
            if ("GET".equals(method)) {
                if ("/".equals(path) || "/demo".equals(path)) {
                    try (InputStream page = getClass().getResourceAsStream("/web/index.html")) {
                        if (page == null) throw new IOException("Missing packaged demo page");
                        exchange.getResponseHeaders().set("Content-Security-Policy",
                                "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'");
                        replies.send(exchange, 200, "text/html; charset=utf-8", new String(page.readAllBytes(), StandardCharsets.UTF_8));
                    }
                    return;
                }
                String[] parts = path.split("/", -1);
                if (parts.length != 4 || !Arithmetic.OPERATIONS.contains(parts[1].toLowerCase(Locale.ROOT))) {
                    replies.sendErrorContent(exchange, Status.NF, "Unknown arithmetic route");
                    return;
                }
                double value = Arithmetic.calculate(parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                replies.sendContentByPlain(exchange, value);
            } else if ("POST".equals(method)) {
                if (!"/".equals(path) && !"/calculate".equals(path)) {
                    replies.sendErrorContent(exchange, Status.NF, "Unknown route");
                    return;
                }
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType != null && !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                    replies.send(exchange, 415, "text/plain; charset=utf-8", "Use application/json\n");
                    return;
                }
                byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
                if (bytes.length > MAX_BODY_BYTES) {
                    replies.send(exchange, 413, "text/plain; charset=utf-8", "Request body exceeds 8192 bytes\n");
                    return;
                }
                String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
                Calculation calculation = parse(json);
                replies.sendContentByJson(exchange, Arithmetic.calculate(calculation.operation(), calculation.left(), calculation.right()));
            } else {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                replies.sendErrorContent(exchange, Status.MNA, "Only GET and POST are supported");
            }
        } catch (CharacterCodingException | IllegalArgumentException | IllegalStateException e) {
            replies.sendErrorContent(exchange, Status.BR, "Invalid input: expected an operation and two finite numbers");
        } finally {
            exchange.close();
        }
    }

    private static Calculation parse(String json) {
        // Read directly with JsonReader: Gson.fromJson would temporarily enable lenient mode.
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            String operation = null;
            double[] values = null;
            Set<String> seen = new HashSet<>();
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName().toLowerCase(Locale.ROOT);
                if (!seen.add(name)) throw new IllegalArgumentException("duplicate field");
                switch (name) {
                    case "operation" -> {
                        if (reader.peek() != JsonToken.STRING) throw new IllegalArgumentException("operation must be a string");
                        operation = Arithmetic.operation(reader.nextString());
                    }
                    case "arguments" -> {
                        values = new double[2];
                        reader.beginArray();
                        for (int i = 0; i < 2; i++) {
                            if (reader.peek() != JsonToken.NUMBER) throw new IllegalArgumentException("two numbers required");
                            values[i] = Double.parseDouble(reader.nextString());
                        }
                        reader.endArray();
                    }
                    default -> throw new IllegalArgumentException("unknown field");
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT || operation == null || values == null)
                throw new IllegalArgumentException("incomplete JSON object");
            return new Calculation(operation, values[0], values[1]);
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed JSON", e);
        }
    }
}
