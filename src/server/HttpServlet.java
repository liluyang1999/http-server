package server;

public abstract class HttpServlet {

    //抽象方法
    public abstract void service(HttpServletRequest request, HttpServletResponse response);

}
