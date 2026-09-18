package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Prints the complete HTTP response so the wire format can be studied. */
public final class Client {
    private Client() {}
    public static void main(String[] args) throws IOException {
        URI uri = URI.create(args.length == 0 ? "http://127.0.0.1:9999/index?username=My%20Sweetheart" : args[0]);
        if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawFragment() != null)
            throw new IllegalArgumentException("Expected an http://host:port/path URL");
        int port = uri.getPort() < 0 ? 80 : uri.getPort();
        String path = uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String target = path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(uri.getHost(), port), 3000);
            socket.setSoTimeout(6000);
            String request = "GET " + target + " HTTP/1.1\r\nHost: " + uri.getHost() + ":" + port + "\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            byte[] response = socket.getInputStream().readNBytes(65537);
            if (response.length > 65536) throw new IOException("Response exceeds demo client's 64 KiB limit");
            System.out.print(new String(response, StandardCharsets.UTF_8));
        }
    }
}
