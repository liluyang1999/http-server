package domain;

public class User {

    private String username;

    private String password;

    private String gender;

    private Boolean isVIP;

    public User() {
        this.username = "user1";
        this.password = "123456";
        this.gender = "Female";
        this.isVIP = Boolean.TRUE;
    }

    public User(String username, String password, String gender, Boolean isVIP) {
        this.username = username;
        this.password = password;
        this.gender = gender;
        this.isVIP = isVIP;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Boolean getVIP() {
        return isVIP;
    }

    public void setVIP(Boolean VIP) {
        isVIP = VIP;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", gender='" + gender + '\'' +
                ", isVIP=" + isVIP +
                '}';
    }
}
