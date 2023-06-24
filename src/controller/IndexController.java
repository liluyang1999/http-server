package controller;

import domain.User;
import server.HttpServlet;
import server.HttpServletRequest;
import server.HttpServletResponse;
import service.UserService;

public class IndexController extends HttpServlet {

    private final UserService userService;

    public IndexController() {
        this.userService = new UserService();
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        User user = userService.getUserByUsername(username);
        if (user != null) {
            response.setContent(user.toString());
        } else {
            response.setContent("404, resource you want can not be found in server......");
        }
    }

}
