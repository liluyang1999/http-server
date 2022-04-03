import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        startHttpServer();
        startHttpsServer();
    }

    public static void startHttpServer() throws Exception {
        MyHttpHandler myHttpHandler = new MyHttpHandler();
        System.out.println("Start the HTTP Server...");
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),8081), 0);
        httpServer.createContext("/", myHttpHandler);
        Executor executor = Executors.newCachedThreadPool();
        httpServer.setExecutor(executor);
        httpServer.start();
        System.out.println("HTTP server starts successfully, port:8081" + "\n");
    }

    public static void startHttpsServer() throws Exception {
        MyHttpHandler myHttpHandler = new MyHttpHandler();
        System.out.println("Start the HTTPS Server...");
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8082), 0);
        httpsServer.createContext("/", myHttpHandler);
        KeyStore keyStore = KeyStore.getInstance("JKS");   //build the key store
        keyStore.load(new FileInputStream("./server.p12"), "123456".toCharArray()); //load the certificate

        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509"); //build the factory of key management
        factory.init(keyStore, "123456".toCharArray());
        KeyManager[] keyManagers = factory.getKeyManagers();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("SSLv3");
        sslContext.init(keyManagers, trustManagers, null);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try
                {
                    //Initialise the SSL context
                    SSLContext sslContext = SSLContext.getDefault ();
                    SSLEngine engine = sslContext.createSSLEngine ();
                    params.setNeedClientAuth (false);
                    params.setCipherSuites (engine.getEnabledCipherSuites());
                    params.setProtocols (engine.getEnabledProtocols());

                    //Achieve the default parameters
                    SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters ();
                    params.setSSLParameters ( defaultSSLParameters );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        httpsServer.start();
        System.out.println("HTTPS server starts successfully, port:8082" + "\n");
    }

    public static final Set<String> operations = new HashSet<>() {
        {
            add("add");
            add("subtract");
            add("multiply");
            add("divide");
        }
    };

}

