package controller;

import domain.User;
import server.HttpServlet;
import server.HttpServletRequest;
import server.HttpServletResponse;
import service.UserService;

public final class IndexController extends HttpServlet {
    private final UserService userService = new UserService();
    @Override public void service(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        if (username == null || username.isBlank()) {
            response.setStatus(400);
            response.setContent("username is required\n");
            return;
        }
        User user = userService.getUserByUsername(username);
        if (user == null) {
            response.setStatus(404);
            response.setContent("User not found\n");
        } else response.setContent(user.toString() + "\n");
    }
}
