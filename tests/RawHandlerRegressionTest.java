import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import server.Handler;

/** Checks the small socket implementation against actual HTTP bytes. */
public final class RawHandlerRegressionTest {
    public static void main(String[] args) throws Exception {
        List<String> failures = new ArrayList<>();
        check(failures, "/index?username=My+Sweetheart", 200, true);
        check(failures, "/index", 400, false);
        check(failures, "/absent", 404, false);
        check(failures, "/index?username", 400, false);
        check(failures, "/index?username=user1&username=user2", 400, false);
        check(failures, "/index?username=My%20Sweetheart", 200, true);
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        System.out.println("RAW_HANDLER_REGRESSIONS_OK 6");
    }

    private static void check(List<String> failures, String target, int status, boolean checkUser) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))) {
            FutureTask<Void> serving = new FutureTask<>(() -> {
                try (Socket accepted = listener.accept()) {
                    accepted.setSoTimeout(1000);
                    new Handler(accepted).run();
                }
                return null;
            });
            Thread worker = new Thread(serving, "raw-regression");
            worker.start();
            String response;
            try (Socket client = new Socket("127.0.0.1", listener.getLocalPort())) {
                client.setSoTimeout(2000);
                client.getOutputStream().write(("GET " + target + " HTTP/1.1\r\nHost: localhost\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                client.shutdownOutput();
                response = new String(client.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            try { serving.get(3, TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException e) { failures.add(target + " handler crashed: " + e.getCause().getClass().getSimpleName()); }
            if (!response.startsWith("HTTP/1.1 " + status + " ")) failures.add(target + " missing HTTP status " + status);
            if (checkUser && (!response.contains("My Sweetheart") || response.contains("password"))) failures.add(target + " incorrect public user response");
        }
    }
}
