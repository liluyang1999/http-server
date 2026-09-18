import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/** Owns listeners and workers together, including partial-startup failure. */
public final class ServerRuntime implements AutoCloseable {
    private final HttpServer http;
    private final HttpsServer https;
    private final ThreadPoolExecutor workers;
    private boolean closed;

    private ServerRuntime(HttpServer http, HttpsServer https, ThreadPoolExecutor workers) {
        this.http = http; this.https = https; this.workers = workers;
    }
    public static ServerRuntime start(int httpPort, Integer httpsPort, Path keyStorePath, char[] password)
            throws IOException, GeneralSecurityException {
        configureLimits();
        SSLContext tls = httpsPort == null ? null : tlsContext(keyStorePath, password);
        ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.CallerRunsPolicy());
        HttpServer http = null;
        HttpsServer https = null;
        try {
            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            http = HttpServer.create(new InetSocketAddress(loopback, httpPort), 32);
            http.createContext("/", new MyHttpHandler());
            http.setExecutor(workers);
            // Start the dispatcher before any later bind can fail. On Windows/JDK 25,
            // stopping a bound but never-started server can leave its selector holding the port.
            http.start();
            if (httpsPort != null) {
                https = HttpsServer.create(new InetSocketAddress(loopback, httpsPort), 32);
                https.setHttpsConfigurator(new HttpsConfigurator(tls) {
                    @Override public void configure(HttpsParameters parameters) {
                        SSLParameters defaults = getSSLContext().getDefaultSSLParameters();
                        defaults.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
                        parameters.setSSLParameters(defaults);
                    }
                });
                https.createContext("/", new MyHttpHandler());
                https.setExecutor(workers);
            }
            if (https != null) https.start();
            return new ServerRuntime(http, https, workers);
        } catch (IOException | RuntimeException e) {
            if (https != null) https.stop(0);
            if (http != null) http.stop(0);
            workers.shutdownNow();
            throw e;
        }
    }
    private static SSLContext tlsContext(Path path, char[] password) throws IOException, GeneralSecurityException {
        if (path == null || password == null || password.length == 0)
            throw new IllegalArgumentException("TLS requires a PKCS12 keystore and a password environment variable");
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(path)) { store.load(input, password); }
        KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(store, password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(managers.getKeyManagers(), null, null);
        return context;
    }
    private static void configureLimits() {
        defaultProperty("jdk.httpserver.maxConnections", "64");
        defaultProperty("sun.net.httpserver.maxIdleConnections", "0");
        defaultProperty("sun.net.httpserver.maxReqHeaders", "32");
        defaultProperty("sun.net.httpserver.maxReqHeaderSize", "8192");
        // In JDK 25 these values are in milliseconds. Validate other JDKs separately.
        defaultProperty("sun.net.httpserver.maxReqTime", "10000");
        defaultProperty("sun.net.httpserver.maxRspTime", "10000");
    }
    private static void defaultProperty(String name, String value) {
        if (System.getProperty(name) == null) System.setProperty(name, value);
    }
    public int httpPort() { return http.getAddress().getPort(); }
    public int httpsPort() { return https == null ? -1 : https.getAddress().getPort(); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        // HttpServer.stop may consume an interrupt; retain the caller's cancellation signal.
        boolean interrupted = Thread.interrupted();
        try {
            if (https != null) https.stop(0);
            http.stop(0);
            interrupted |= Thread.interrupted();
            workers.shutdownNow();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!workers.isTerminated()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IllegalStateException("HTTP workers did not stop");
                try { workers.awaitTermination(remaining, TimeUnit.NANOSECONDS); }
                catch (InterruptedException e) { interrupted = true; }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
