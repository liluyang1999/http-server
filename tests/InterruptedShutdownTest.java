import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import server.Server;

/** Shutdown must finish even when the caller already carries a cancellation signal. */
public final class InterruptedShutdownTest {
    public static void main(String[] args) throws Exception {
        List<String> failures = new ArrayList<>();
        ServerRuntime jdk = ServerRuntime.start(0, null, null, null);
        int jdkPort = jdk.httpPort();
        try (Socket idle = new Socket("127.0.0.1", jdkPort)) {
            idle.setSoTimeout(2000);
            closeInterrupted(jdk, "JDK", failures);
        } finally { jdk.close(); }
        rebind(jdkPort);

        Server raw = new Server(0, 1, 1, 5000);
        int rawPort = raw.port();
        try (Socket idle = new Socket("127.0.0.1", rawPort)) {
            idle.setSoTimeout(2000);
            closeInterrupted(raw, "Socket", failures);
            if (raw.activeConnections() != 0) failures.add("Socket retains tracked connections");
        } finally { raw.close(); }
        rebind(rawPort);
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        System.out.println("INTERRUPTED_SHUTDOWN_OK 5");
    }

    private static void closeInterrupted(AutoCloseable server, String label, List<String> failures) {
        Thread.currentThread().interrupt();
        try {
            server.close();
        } catch (Exception e) {
            failures.add(label + " close interrupted instead of completing: " + e.getClass().getSimpleName());
        } finally {
            if (!Thread.interrupted()) failures.add(label + " lost the caller's interrupt status");
        }
    }

    private static void rebind(int port) throws Exception {
        try (ServerSocket listener = new ServerSocket(port, 4, InetAddress.getByName("127.0.0.1"))) {
            if (!listener.isBound()) throw new AssertionError("Port was not released");
        }
    }
}
