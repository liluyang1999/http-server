import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Date;

public class HandlerUtil {

    public void sendContentByPlain(HttpExchange exchange, Number result) {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Date", Date.from(Instant.now()).toString());
        responseHeaders.set("Protocol", exchange.getProtocol());
        responseHeaders.set("Content-Type", "text/plain");
        responseHeaders.set("expires", "-1");
        try {
            exchange.sendResponseHeaders(Status.OK.getCode(), 0);
            OutputStream responseBody = exchange.getResponseBody();
            String content = exchange.getProtocol() + " "
                    + Status.OK.getCode() + " "+ Status.OK.getStatus() + "\n" +
                    """
                     Content-Type: text/plain
                          
                     """
                    + result + "\n";
            responseBody.write(content.getBytes());
            responseBody.close();
        } catch (IOException e) {
            sendErrorContent(exchange, Status.IE, MyHttpHandler.INTERNAL_SERVER_ERROR);
        }
    }

    public void sendContentByJson(HttpExchange exchange, Number result) {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Date", Date.from(Instant.now()).toString());
        responseHeaders.set("Protocol", exchange.getProtocol());
        responseHeaders.set("Content-Type", "application/json");
        responseHeaders.set("Expires", "-1");
        try {
            exchange.sendResponseHeaders(Status.OK.getCode(), 0);
            OutputStream responseBody = exchange.getResponseBody();
            String content = exchange.getProtocol() + " "
                    + Status.OK.getCode() + " "+ Status.OK.getStatus() + "\n" +
                    """
                     Content-Type: application/json
                          
                     """
                     + toJsonFormat(result) + "\n";
            responseBody.write(content.getBytes());
            responseBody.close();
        } catch (IOException e) {
            sendErrorContent(exchange, Status.IE, MyHttpHandler.INTERNAL_SERVER_ERROR);
        }
    }

    public void sendErrorContent(HttpExchange exchange, Status status, String description) {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Protocol", exchange.getProtocol());
        responseHeaders.set("Expires", "-1");
        try {
            exchange.sendResponseHeaders(status.getCode(), 0);
            OutputStream responseBody = exchange.getResponseBody();
            String content = exchange.getProtocol() + " "
                            + status.getCode() + " "+ status.getStatus() + "\n"
                            + "\n" + description + "\n";
            responseBody.write(content.getBytes());
            responseBody.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String toJsonFormat(Number result) {
        return "{" + "\n"
                + "   \"result\": " + result + "\n"
                + "}" + "\n";
    }

    public static class Params {
        public String operation;
        public Double[] arguments = new Double[10];
    }

}

