package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Listener, bounded workers, queued sockets and shutdown share one owner. */
public final class Server implements AutoCloseable {
    private final ServerSocket listener;
    private final ThreadPoolExecutor workers;
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final Thread acceptor;
    private final int timeoutMillis;
    private volatile boolean closed;

    public Server() throws IOException { this(9999, 4, 16, 5000); }
    public Server(int port, int workerCount, int queueSize, int timeoutMillis) throws IOException {
        if (workerCount < 1 || queueSize < 1 || timeoutMillis < 1)
            throw new IllegalArgumentException("Positive worker, queue and deadline values required");
        MyServerReader.validate(); // Fail before binding when the classpath configuration is broken.
        this.timeoutMillis = timeoutMillis;
        workers = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize));
        listener = new ServerSocket();
        try { listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), queueSize); }
        catch (IOException | RuntimeException e) { listener.close(); workers.shutdownNow(); throw e; }
        acceptor = new Thread(this::acceptLoop, "teaching-http-accept");
        acceptor.start();
    }
    public int port() { return listener.getLocalPort(); }
    public int activeConnections() { return sockets.size(); }
    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = listener.accept();
                sockets.add(socket);
                // Construct now so queue time is included in the header deadline.
                Handler handler = new Handler(socket, timeoutMillis);
                try {
                    workers.execute(() -> {
                        try { handler.run(); }
                        finally { sockets.remove(socket); }
                    });
                } catch (RejectedExecutionException e) {
                    sockets.remove(socket);
                    closeSocket(socket); // No blocking write on the acceptor during overload.
                }
            } catch (IOException e) {
                if (!closed) System.err.println("Socket listener failed: " + e.getClass().getSimpleName());
                break;
            }
        }
    }
    private static void closeSocket(Socket socket) {
        try { socket.close(); } catch (IOException e) { /* Best effort during shutdown. */ }
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        boolean interrupted = Thread.interrupted();
        try { listener.close(); } catch (IOException e) { /* Workers and sockets still need closing. */ }
        // An accept may race with the first snapshot; joining prevents any further additions.
        sockets.forEach(Server::closeSocket);
        workers.shutdownNow();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (acceptor.isAlive()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                try { TimeUnit.NANOSECONDS.timedJoin(acceptor, remaining); }
                catch (InterruptedException e) { interrupted = true; }
            }
            sockets.forEach(Server::closeSocket);
            sockets.clear();
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!workers.isTerminated()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                try { workers.awaitTermination(remaining, TimeUnit.NANOSECONDS); }
                catch (InterruptedException e) { interrupted = true; }
            }
            if (acceptor.isAlive() || !workers.isTerminated())
                throw new IllegalStateException("Socket server did not stop");
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Usage: server.Server [port]");
        int port = args.length == 0 ? 9999 : Integer.parseInt(args[0]);
        try (Server server = new Server(port, 4, 16, 5000)) {
            Thread hook = new Thread(server::close, "socket-shutdown");
            Runtime.getRuntime().addShutdownHook(hook);
            System.out.println("Socket teaching server: http://127.0.0.1:" + server.port() + "/index?username=My%20Sweetheart");
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
