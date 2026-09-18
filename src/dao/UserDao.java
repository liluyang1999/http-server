package dao;

import domain.User;

public class UserDao {

    public User selectUser() {
        // Fixed teaching data: no database connection or password authentication.
        return new User("My Sweetheart", null, "Female", true);
    }

}
