package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        Client client = new Client();
        client.openBrowser();
    }

    //1.打开浏览器
    //2.输入一个URL  ip:port/content?key=value
    //3.解析URL   ip, port, content?key=value
    //4.创建连接    ip, port, content?key=value
    //5.发送请求(写out给服务器)
    //6.读取服务器写回来的响应信息(String)
    //7.解析响应信息并浏览器中展示出来

    public void openBrowser() {
        System.out.println("======打开浏览器======");
        System.out.print("请输入URL：");
        Scanner scanner = new Scanner(System.in);
        String url = scanner.nextLine();

        this.parseURL(url);
    }

    private void parseURL(String url) {
        int index1 = url.indexOf(":"); //第一次冒号
        int index2 = url.indexOf("/"); //第一次斜杠
        if (index1 == -1 || index2 == -1) {
            System.out.println("请输入完整的URL！！！");
        } else {
            String ip = url.substring(0, index1);
            Integer port = Integer.valueOf(url.substring(index1 + 1, index2));
            String content = url.substring(index2); // /content?key=value
            this.sendRequest(ip, port, content);
        }
    }

    private void sendRequest(String ip, Integer port, String content) {
        try (Socket socket = createConnection(ip, port)) {
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(content);
            writer.flush();

            //等着服务器发回响应信息
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String result = reader.readLine();
            System.out.println(result);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private Socket createConnection(String ip, Integer port) throws IOException {
        return new Socket(ip, port);
    }

}
