import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import server.Server;

public final class SocketIntegrationTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        try (Server server = new Server(0, 2, 4, 1000)) {
            String good = request(server, "GET /index?username=My+Sweetheart HTTP/1.1\r\nHost: localhost\r\n\r\n");
            require(good.startsWith("HTTP/1.1 200 "), "normal HTTP");
            require(good.contains("My Sweetheart") && !good.contains("password"), "public fixture");
            int separator = good.indexOf("\r\n\r\n");
            int bodyBytes = good.substring(separator + 4).getBytes(StandardCharsets.UTF_8).length;
            require(good.contains("Content-Length: " + bodyBytes + "\r\n"), "UTF-8 body length");
            status(server, "GET /index HTTP/1.1\r\n\r\n", 400);
            status(server, "POST /index HTTP/1.1\r\nHost: localhost\r\n\r\n", 405);
            String head = request(server, "HEAD /index HTTP/1.1\r\nHost: localhost\r\n\r\n");
            require(head.startsWith("HTTP/1.1 405 ") && head.endsWith("\r\n\r\n"), "HEAD has no body");
            status(server, "GET /index HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n", 501);
            status(server, "GET /index?username=%FF HTTP/1.1\r\nHost: localhost\r\n\r\n", 400);
            status(server, "GET /index?username=%EF%BF%BD HTTP/1.1\r\nHost: localhost\r\n\r\n", 404);
            status(server, "GET /index?username=%E4%B8%AD HTTP/1.1\r\nHost: localhost\r\n\r\n", 404);
            status(server, "GET /index?username=%C0%AF HTTP/1.1\r\nHost: localhost\r\n\r\n", 400);
            status(server, "GET /index?username=%ED%A0%80 HTTP/1.1\r\nHost: localhost\r\n\r\n", 400);
            status(server, "GET /index?username=%00 HTTP/1.1\r\nHost: localhost\r\n\r\n", 400);
            status(server, "GET /index HTTP/1.1\r\nHost: localhost\r\nHost: other\r\n\r\n", 400);
            status(server, "GET /index HTTP/1.1\r\nHost: localhost\r\nContent-Length: 1\r\n\r\nx", 400);
            status(server, "GET /index HTTP/1.1\nHost: localhost\n\n", 400);
            status(server, "GET /" + "x".repeat(2100) + " HTTP/1.1\r\nHost: localhost\r\n\r\n", 431);
            status(server, "GET /index HTTP/1.1\r\nHost: localhost\r\n", 408);
            require(request(server, "").startsWith("HTTP/1.1 408 "), "idle connection deadline");
        }
        List<Socket> sockets = new ArrayList<>();
        Server bounded = new Server(0, 1, 1, 5000);
        try {
            for (int i = 0; i < 2; i++) {
                Socket socket = new Socket("127.0.0.1", bounded.port());
                socket.setSoTimeout(2000);
                socket.getOutputStream().write('G');
                sockets.add(socket);
                long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (bounded.activeConnections() < i + 1 && System.nanoTime() < until) Thread.sleep(5);
                require(bounded.activeConnections() == i + 1, "accepted controlled slow client");
            }
            try (Socket rejected = new Socket("127.0.0.1", bounded.port())) {
                rejected.setSoTimeout(2000);
                require(rejected.getInputStream().read() == -1, "overload closes surplus connection");
            }
            bounded.close();
            require(bounded.activeConnections() == 0, "queued and active sockets released");
            for (Socket socket : sockets) {
                try { require(socket.getInputStream().read() == -1, "shutdown closes client"); }
                catch (java.net.SocketException e) { require(true, "shutdown reset is also closed"); }
            }
        } finally {
            bounded.close();
            for (Socket socket : sockets) socket.close();
        }
        System.out.println("SOCKET_INTEGRATION_OK " + checks);
    }
    private static String request(Server server, String text) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.port())) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static void status(Server server, String request, int code) throws Exception {
        require(request(server, request).startsWith("HTTP/1.1 " + code + " "), "expected " + code);
    }
    private static void require(boolean value, String name) {
        checks++;
        if (!value) throw new AssertionError(name);
    }
}
