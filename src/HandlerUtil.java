import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** HTTP framing belongs to HttpExchange; only representation bytes go into its body. */
public final class HandlerUtil {
    public void sendContentByPlain(HttpExchange exchange, Number result) throws IOException {
        send(exchange, 200, "text/plain; charset=utf-8", result + "\n");
    }
    public void sendContentByJson(HttpExchange exchange, Number result) throws IOException {
        send(exchange, 200, "application/json; charset=utf-8", "{\"result\":" + result + "}\n");
    }
    public void sendErrorContent(HttpExchange exchange, Status status, String description) throws IOException {
        send(exchange, status.getCode(), "text/plain; charset=utf-8", description + "\n");
    }
    public void send(HttpExchange exchange, int status, String type, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Connection", "close");
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        }
    }
}
