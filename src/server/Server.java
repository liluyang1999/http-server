package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    //1.启动服务器
    //2.等待浏览器过来创建连接 socket
    //3.读取浏览器发送过来的请求信息 --> 请求
    //4.解析请求信息 content, key = value String, map
    //5.用请求名字，找寻资源（文件/操作）   Java类（方法--做事）
    //6.资源执行完的结果，再重新写回浏览器 --> 响应
    @SuppressWarnings("InfiniteLoopStatement")
    public void startServer() {
        System.out.println("===启动服务器===");
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            while(true) {
                Socket socket = serverSocket.accept();

                //开辟一个线程去处理这个请求
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

}
