package server;

import java.util.Objects;

public final class HttpServletResponse {
    private int status = 200;
    private String content = "";
    public int getStatus() { return status; }
    public void setStatus(int status) {
        if (status < 100 || status > 599) throw new IllegalArgumentException("Invalid HTTP status");
        this.status = status;
    }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = Objects.requireNonNull(content); }
}
