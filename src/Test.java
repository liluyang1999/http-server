import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Test {

    public void exchangeHttp(HttpExchange exchange) {
        List<String> list = new LinkedList<>();
        System.out.println(list.stream().allMatch(e -> e.equals("a")));
        System.out.println(list.stream().anyMatch(e -> e.equals("a")));
        Stream<String> result = list.stream().filter(e -> e.equals("a"));
        System.out.println(result.allMatch(e -> e.equals("a")));
        result = list.stream().filter(e -> e.equals("a"));
        System.out.println(result.anyMatch(e -> e.equals("a")));
    }

    public static void main(String[] args) throws IOException {
        BufferedReader stream = new BufferedReader(new InputStreamReader(System.in));
        InputStreamReader reader = new InputStreamReader(System.in);
        System.out.println(stream.readLine());
        System.out.println(reader.read());
    }

}
