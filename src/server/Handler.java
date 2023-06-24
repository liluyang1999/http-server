package server;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Handler extends Thread {

    private final Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        this.receiveRequest(this.socket);
    }

    private void receiveRequest(Socket socket) {
        try {
            //拿到输入流得到请求内容
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String content = reader.readLine();
            System.out.println("content: " + content);
            if (!content.isBlank()) {
                //解析请求内容，计算出处理结果存入response并返回
                HttpServletResponse response = parseContent(content);

                //回的输出流将处理结果返回给客户端
                OutputStream serverOutputStream = socket.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(serverOutputStream));
                if (response.getContent() != null) {
                    writer.write(response.getContent());
                    writer.flush();
                }
                writer.close();
                reader.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private HttpServletResponse parseContent(String content) {
        content = content.substring(content.indexOf("/") + 1);

        int index = content.indexOf("?");
        String requestName;
        Map<String, String> paramsMap = new HashMap<>();
        if (index == -1) {
            requestName = content;
        } else {
            requestName = content.substring(0, index);
            paramsMap = new HashMap<>();
            for (String each : content.substring(index + 1).split("&")) {
                int loc = each.indexOf("=");
                String key = each.substring(0, loc);
                String value = each.substring(loc + 1);
                paramsMap.put(key, value);
            }
        }

        HttpServletRequest request = new HttpServletRequest(requestName, paramsMap);
        HttpServletResponse response = new HttpServletResponse();
        request.setRequestName(requestName);
        findServlet(request, response);
        return response;
    }

    private void findServlet(HttpServletRequest request, HttpServletResponse response) {
        //获得请求名
        String requestName = request.getRequestName();
        HttpServlet httpServlet = MyServerReader.getController(requestName);
        if (httpServlet != null) {
            try {
                //通过反射的方式获得这个类的信息以及实例
                Class<?> clazz = httpServlet.getClass();
                Method method = clazz.getMethod("service", HttpServletRequest.class, HttpServletResponse.class);
                httpServlet = (HttpServlet) clazz.getConstructor().newInstance();
                method.invoke(httpServlet, request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            response.setContent("404, the resource is not found......");
        }
    }

}
