package service;

import dao.UserDao;
import domain.User;

public class UserService {

    private UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    // Look up the fixed demonstration user; this is not a login operation.
    public User getUserByUsername(String username) {
        User user = userDao.selectUser();
        if (user.getUsername().equals(username)) {
            return user;
        } else {
            return null;
        }
    }

}
