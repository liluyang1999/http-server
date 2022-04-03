import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class MyHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        String requestMethod = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(requestMethod)) {
            //The request method is "GET"
            URI requestURI = exchange.getRequestURI();
            String path = requestURI.getRawPath();
            String operation;
            String[] params;
            params = path.split("/");

            if(params.length == 0 || params.length == 1) {
                handlerUtil.sendErrorContent(exchange, Status.NF, NOT_FOUND);
                return;
            }

            operation = params[1];
            if (!Main.operations.contains(operation)) {
                //Arithmetic method can't be recognized
                handlerUtil.sendErrorContent(exchange, Status.NF, NOT_FOUND);
            } else {
                //Check the arithmetic arguments
                if(params.length == 4) {
                    try {
                        Double argument1 = Double.parseDouble(params[2]);
                        Double argument2 = Double.parseDouble(params[3]);
                        Number result = calculate(argument1, argument2, operation);
                        handlerUtil.sendContentByPlain(exchange, result);
                    } catch (NumberFormatException e) {
                        handlerUtil.sendErrorContent(exchange, Status.BR, BAD_REQUEST);
                    }
                } else {
                    handlerUtil.sendErrorContent(exchange, Status.NF, NOT_FOUND);
                }
            }
        } else if ("POST".equalsIgnoreCase(requestMethod)) {
            //The request method is "POST"
            try {
                HandlerUtil.Params params;
                InputStream requestBody = exchange.getRequestBody();
                String paramsJsonString = new String(requestBody.readAllBytes()).trim();
                try {
                    params = new Gson().fromJson(paramsJsonString, HandlerUtil.Params.class);
                    if(params.operation == null || !Main.operations.contains(params.operation)
                            || params.arguments == null || params.arguments.length != 2) {
                        handlerUtil.sendErrorContent(exchange, Status.BR, BAD_REQUEST);
                    } else {
                        Number result = calculate(params.arguments[0], params.arguments[1], params.operation);
                        handlerUtil.sendContentByJson(exchange, result);
                    }
                } catch (Exception e) {
                    handlerUtil.sendErrorContent(exchange, Status.BR, BAD_REQUEST);
                }
            } catch (IOException e) {
                handlerUtil.sendErrorContent(exchange, Status.IE, INTERNAL_SERVER_ERROR);
            }
        } else {
            //The request method can't be processed by the server
            handlerUtil.sendErrorContent(exchange, Status.MNA, METHOD_NOT_ALLOWED);
        }
    }

    private Double calculate(Double param1, Double param2, String operation) {
        String type = operation.toLowerCase();
        double result = 0.0;
        switch (type) {
            case "add" -> result = param1 + param2;
            case "subtract" -> result = param1 - param2;
            case "multiply" -> result = param1 * param2;
            case "divide" -> {
                                if (param2 == 0.0) throw new NumberFormatException();
                                result = param1 / param2;
                              }
        }
        return result;
    }

    private final HandlerUtil handlerUtil;

    public MyHttpHandler() {
        this.handlerUtil = new HandlerUtil();
    }

    public static final String NOT_FOUND = "The requested URL is not found, please check the URL";
    public static final String BAD_REQUEST = "The request can't be fulfilled due to bad syntax in params";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error happens";
    public static final String METHOD_NOT_ALLOWED = "The request method is not allowed by the server";

}
