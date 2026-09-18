import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public final class Main {
    public static final Set<String> operations = Arithmetic.OPERATIONS;
    private Main() {}
    public static void main(String[] args) throws Exception {
        int port = 8081;
        Integer tlsPort = null;
        Path keyStore = null;
        String passwordVariable = "HTTP_TLS_PASSWORD";
        Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < args.length; i++) {
            if ("--help".equals(args[i])) {
                System.out.println("Main [--port 8081] [--https-port 8082 --keystore file.p12 --password-env HTTP_TLS_PASSWORD]");
                return;
            }
            String option = args[i];
            if (!seen.add(option) || i + 1 == args.length) throw new IllegalArgumentException("Duplicate option or missing value: " + option);
            String value = args[++i];
            switch (option) {
                case "--port" -> port = parsePort(value);
                case "--https-port" -> tlsPort = parsePort(value);
                case "--keystore" -> keyStore = Path.of(value);
                case "--password-env" -> passwordVariable = value;
                default -> throw new IllegalArgumentException("Unknown option: " + option);
            }
        }
        if (tlsPort == null && keyStore != null) throw new IllegalArgumentException("--keystore requires --https-port");
        String secret = tlsPort == null ? null : System.getenv(passwordVariable);
        char[] password = secret == null ? null : secret.toCharArray();
        ServerRuntime runtime;
        try { runtime = ServerRuntime.start(port, tlsPort, keyStore, password); }
        finally { if (password != null) Arrays.fill(password, '\0'); }
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close, "http-shutdown"));
        System.out.println("HTTP ready: http://127.0.0.1:" + runtime.httpPort());
        if (runtime.httpsPort() >= 0) System.out.println("HTTPS ready: https://localhost:" + runtime.httpsPort());
        try { new CountDownLatch(1).await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { runtime.close(); }
    }
    private static int parsePort(String value) {
        int port = Integer.parseInt(value);
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Port must be 0..65535");
        return port;
    }
}
