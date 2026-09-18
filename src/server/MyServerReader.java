package server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Immutable route table; controllers are created per request, not shared between workers. */
public final class MyServerReader {
    private static final Map<String, Constructor<? extends HttpServlet>> ROUTES = load();
    private MyServerReader() {}
    private static Map<String, Constructor<? extends HttpServlet>> load() {
        Properties properties = new Properties();
        try (InputStream input = MyServerReader.class.getResourceAsStream("/web.properties")) {
            if (input == null) throw new IOException("Missing web.properties on classpath");
            properties.load(input);
            Map<String, Constructor<? extends HttpServlet>> routes = new HashMap<>();
            for (String name : properties.stringPropertyNames()) {
                Class<? extends HttpServlet> type = Class.forName(properties.getProperty(name).trim()).asSubclass(HttpServlet.class);
                routes.put(name, type.getConstructor());
            }
            return Map.copyOf(routes);
        } catch (IOException | ReflectiveOperationException | ClassCastException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static void validate() { if (ROUTES.isEmpty()) throw new IllegalStateException("No routes configured"); }
    public static HttpServlet getController(String requestName) {
        Constructor<? extends HttpServlet> constructor = ROUTES.get(requestName);
        if (constructor == null) return null;
        try { return constructor.newInstance(); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot instantiate configured controller", e); }
    }
}
