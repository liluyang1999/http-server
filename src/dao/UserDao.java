package dao;

import domain.User;

public class UserDao {

    public User selectUser() {
        //执行JDBC流程从数据库中查询数据并返回
        return new User("My Sweetheart", "123456", "Female", true);
    }

}
