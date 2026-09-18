import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Real TLS with a locally generated certificate, plus listener ownership tests. */
public final class RuntimeIntegrationTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of(System.getProperty("jdk.net.unixdomain.tmpdir")), "tls-test-");
        Path keystore = directory.resolve("server.p12");
        Path log = directory.resolve("keytool.log");
        String password = UUID.randomUUID().toString();
        char[] passwordChars = password.toCharArray();
        int checks = 0;
        try {
            Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool.exe");
            if (!Files.exists(keytool)) keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
            ProcessBuilder builder = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "test",
                    "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12", "-keystore", keystore.toString(),
                    "-storepass:env", "HTTP_TEST_KEY_PASSWORD", "-keypass:env", "HTTP_TEST_KEY_PASSWORD",
                    "-validity", "1", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1");
            builder.environment().put("HTTP_TEST_KEY_PASSWORD", password);
            Process keyProcess = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                if (!keyProcess.waitFor(30, TimeUnit.SECONDS) || keyProcess.exitValue() != 0)
                    throw new AssertionError("Installed keytool failed to create local fixture");
            } finally { if (keyProcess.isAlive()) keyProcess.destroyForcibly().waitFor(); }
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream input = Files.newInputStream(keystore)) { trustStore.load(input, passwordChars); }
            TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trust.getTrustManagers(), null);
            HttpClient client = HttpClient.newBuilder().sslContext(context).connectTimeout(Duration.ofSeconds(3)).build();
            int httpPort;
            int httpsPort;
            ServerRuntime runtime = ServerRuntime.start(0, 0, keystore, passwordChars);
            try {
                httpPort = runtime.httpPort(); httpsPort = runtime.httpsPort();
                for (String scheme : new String[]{"http", "https"}) {
                    int port = scheme.equals("http") ? httpPort : httpsPort;
                    HttpResponse<String> reply = client.send(HttpRequest.newBuilder(URI.create(scheme + "://localhost:" + port + "/add/2/3"))
                            .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
                    require(reply.statusCode() == 200 && reply.body().equals("5.0\n"), scheme + " calculation"); checks++;
                    if (scheme.equals("https")) {
                        String protocol = reply.sslSession().orElseThrow().getProtocol();
                        require(protocol.equals("TLSv1.3") || protocol.equals("TLSv1.2"), "modern TLS"); checks++;
                    }
                }
                var page = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + httpPort + "/"))
                        .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
                require(page.statusCode() == 200 && page.body().contains("HTTP 算术实验室"), "packaged page"); checks++;
            } finally { runtime.close(); }
            runtime.close(); // Idempotence is part of the ownership contract.
            rebind(httpPort); rebind(httpsPort); checks += 2;

            int failedHttpPort;
            try (ServerSocket available = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))) { failedHttpPort = available.getLocalPort(); }
            try (ServerSocket occupied = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))) {
                boolean rejected = false;
                try (ServerRuntime unexpected = ServerRuntime.start(failedHttpPort, occupied.getLocalPort(), keystore, passwordChars)) {
                    throw new AssertionError("Occupied HTTPS port unexpectedly bound: " + unexpected.httpsPort());
                } catch (java.net.BindException e) { rejected = true; }
                require(rejected, "occupied port rejected");
                rebind(failedHttpPort); checks++;
            }
            boolean rejected = false;
            try (ServerRuntime unexpected = ServerRuntime.start(0, 0, keystore, new char[]{'x'})) {
                throw new AssertionError("Wrong password accepted: " + unexpected.httpPort());
            } catch (java.io.IOException e) { rejected = true; }
            require(rejected, "wrong keystore password rejected"); checks++;
            System.out.println("RUNTIME_INTEGRATION_OK " + checks);
        } finally {
            Arrays.fill(passwordChars, '\0');
            Files.deleteIfExists(log); Files.deleteIfExists(keystore); Files.delete(directory);
        }
    }
    private static void rebind(int port) throws Exception {
        try (ServerSocket listener = new ServerSocket(port, 4, InetAddress.getByName("127.0.0.1"))) {
            require(listener.isBound(), "port released");
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
