import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Real HTTP regressions; uses only the JDK, without a downloaded test runner. */
public final class HttpRegressionTest {
    private static final List<String> failures = new ArrayList<>();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();
    private static int checks;
    private static String base;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 16);
        server.createContext("/", new MyHttpHandler());
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
        try {
            response("GET", "/add/7/8", null, 200, "15.0\n");
            response("POST", "/", "{\"operation\":\"divide\",\"arguments\":[9,3]}", 200, "{\"result\":3.0}\n");
            response("POST", "/", "{\"OPERATION\":\"ADD\",\"ARGUMENTS\":[1,2]}", 200, "{\"result\":3.0}\n");
            response("GET", "/add/NaN/1", null, 400, null);
            response("GET", "/multiply/1e308/1e308", null, 400, null);
            response("GET", "/divide/2/0", null, 400, null);
            response("GET", "/missing/1/2", null, 404, null);
            response("POST", "/", "null", 400, null);
            response("POST", "/", "{'operation':'add','arguments':[1,2]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",\"arguments\":[null,2]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",\"arguments\":[1,2]} true", 400, null);
            response("POST", "/", " ".repeat(8193), 413, null);
            response("PUT", "/", "", 405, null);
            response("GET", "/ADD/-7/8", null, 200, "1.0\n");
            response("GET", "/subtract/2.5/4", null, 200, "-1.5\n");
            response("GET", "/divide/2/-0.0", null, 400, null);
            response("GET", "/add/Infinity/1", null, 400, null);
            response("GET", "/add/1", null, 404, null);
            response("POST", "/absent", "{}", 404, null);
            response("POST", "/", "{\"operation\":\"add\",\"arguments\":[1,2,3]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",\"arguments\":[\"1\",2]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",\"arguments\":[1e309,2]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",\"Operation\":\"divide\",\"arguments\":[1,2]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",\"arguments\":[1,2],\"extra\":1}", 400, null);
            response("POST", "/", "{\"arguments\":[1,2]}", 400, null);
            response("POST", "/", "{\"operation\":\"add\",/*comment*/\"arguments\":[1,2]}", 400, null);
            response("HEAD", "/", null, 405, "");
            String valid = "{\"arguments\":[1,2],\"operation\":\"add\"}";
            response("POST", "/calculate", valid + " ".repeat(8192 - valid.length()), 200, "{\"result\":3.0}\n");
            rawBody(new byte[]{(byte) 0xC3, (byte) 0x28}, "application/json", 400);
            rawBody(valid.getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/plain", 415);
        } finally {
            server.stop(0);
        }
        if (!failures.isEmpty()) {
            failures.forEach(System.err::println);
            throw new AssertionError(failures.size() + " of " + checks + " HTTP checks failed");
        }
        System.out.println("HTTP_REGRESSIONS_OK " + checks);
    }

    private static void rawBody(byte[] body, String type, int status) throws Exception {
        var reply = client.send(HttpRequest.newBuilder(URI.create(base + "/calculate"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", type)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.ofString());
        checks++;
        if (reply.statusCode() != status) failures.add("raw body expected " + status + ", got " + reply.statusCode());
    }

    private static void response(String method, String path, String body, int status, String expected) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) builder.header("Content-Type", "application/json");
        HttpResponse<String> actual = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        checks++;
        if (!actual.headers().firstValue("Content-Type").orElse("").contains("charset=utf-8")) failures.add("missing UTF-8 response type");
        if (status == 405 && !actual.headers().firstValue("Allow").orElse("").equals("GET, POST")) failures.add("missing Allow header");
        if (!method.equals("HEAD") && actual.headers().firstValueAsLong("Content-Length").orElse(-1)
                != actual.body().getBytes(java.nio.charset.StandardCharsets.UTF_8).length) failures.add("incorrect Content-Length");
        if (actual.statusCode() != status || (expected != null && !actual.body().equals(expected))) {
            failures.add(method + " " + path + ": expected " + status + " / " + expected
                    + ", got " + actual.statusCode() + " / " + actual.body());
        }
    }
}
