package server;

import java.util.HashMap;
import java.util.Map;

/** Parameters belong to this request; the caller's map is not shared. */
public final class HttpServletRequest {
    private String requestName;
    private final Map<String, String> paramsMap;
    public HttpServletRequest(String requestName, Map<String, String> paramsMap) {
        this.requestName = requestName;
        this.paramsMap = new HashMap<>(paramsMap);
    }
    public String getRequestName() { return requestName; }
    public void setRequestName(String requestName) { this.requestName = requestName; }
    public String getParameter(String key) { return paramsMap.get(key); }
    public void setParameter(String key, String value) { paramsMap.put(key, value); }
}
