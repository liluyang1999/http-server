package server;

import java.util.HashMap;
import java.util.Map;

public class HttpServletRequest {

    private String requestName;
    private Map<String, String> paramsMap;

    public HttpServletRequest(String requestName, Map<String, String> paramsMap) {
        this.requestName = requestName;
        this.paramsMap = paramsMap;
    }

    public String getRequestName() {
        return requestName;
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public String getParameter(String key) {
        return paramsMap.get(key);
    }

    public void setParameter(String key, String value) {
        this.paramsMap.put(key, value);
    }
}
