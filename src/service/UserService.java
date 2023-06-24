package service;

import dao.UserDao;
import domain.User;

public class UserService {

    private UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    //登陆业务方法
    public User getUserByUsername(String username) {
        User user = userDao.selectUser();
        if (user.getUsername().equals(username)) {
            return user;
        } else {
            return null;
        }
    }

}

//"HTTP1.1 200 OK\r\n" + "Content-Type: text/html;charset=UTF-8\r\n\r\n" +
//        """
//                    <!DOCTYPE html>
//                    <html lang="en">
//                    <head>
//                        <meta charset="UTF-8">
//                        <title>Fucking Title</title>
//                    </head>
//                    <body>
//                """ +
//        username + " " + password +
//        """
//                    <Input type="button" value="按钮">
//                    </body>
//                    </html>
//                """;
