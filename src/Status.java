public enum Status {

    OK(200, "OK"),
    PR(301, "Permanent Redirect"),
    TR(302, "Temporary Redirect"),
    BR(400, "Bad Request"),
    NF(404, "Not Found"),
    MNA(405, "Method Not Allowed"),
    IE(500, "Internal Server Error"),
    SU(503, "Service Unavailable");

    private int code;
    private String status;

    Status(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
