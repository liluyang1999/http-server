package server;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//服务器启动时候读取文件信息
//配置文件 -> 请求名字-真实类名字
public class MyServerReader {

    //collection map
    private static Map<String, String> configMap;

    private static Map<String, HttpServlet> controllerMap;

    static {
        try {
            MyServerReader.configMap = new HashMap<>();
            MyServerReader.controllerMap = new HashMap<>();

            Properties prop = new Properties();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("./web.properties");
            prop.load(is);
            Enumeration<?> en = prop.propertyNames();
            while (en.hasMoreElements()) {
                String key = (String) en.nextElement();
                String value = prop.getProperty(key);
                configMap.put(key, value);
            }

            assert is != null;
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HttpServlet getController(String requestName) {
        //根据请求的名字找到对应的资源类下的方法
        //index.jsp -> controller
        //参考说明书 读取配置文件 通过请求名得到真实类全名
        HttpServlet controller = controllerMap.get(requestName);

        if (controller == null) {
            System.out.println("没有发现，去配置里拿。。");
            try {
                String className = configMap.get(requestName);
                //通过类名反射加载，得到实体后存入缓存Map中
                if (className != null) {
                    Class<?> clazz = Class.forName(className);
                    controller = (HttpServlet) clazz.getConstructor().newInstance();
                    controllerMap.put(requestName, controller);
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Controller: " + controller);
        } else {
            System.out.println("直接拿到：" + controller);
        }

        return controller;
    }

}
