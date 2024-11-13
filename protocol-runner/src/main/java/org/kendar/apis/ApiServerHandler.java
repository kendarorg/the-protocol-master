package org.kendar.apis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.kendar.apis.dtos.Ko;
import org.kendar.utils.JsonMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApiServerHandler implements HttpHandler {
    private final ApiHandler handler;
    private static JsonMapper mapper = new JsonMapper();

    public ApiServerHandler(ApiHandler handler){

        this.handler = handler;
    }
    private boolean isPath(String path, String api, Map<String, String> parameters) {
        try {
            if (path.equalsIgnoreCase(api) ||
                    path.equalsIgnoreCase(api + "/")) return true;
            var explodedPath = path.split("/");
            var explodedApi = api.split("/");
            if (explodedPath.length != explodedApi.length) return false;
            for (int i = 0; i < explodedPath.length; i++) {
                var expl = explodedPath[i];
                var explApi = explodedApi[i];
                if (explApi.startsWith("{") && explApi.endsWith("}")) {
                    parameters.put(explApi.substring(1, explApi.length() - 1), expl);
                } else if (!explApi.equalsIgnoreCase(expl)) {
                    return false;
                }
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private boolean isPath(String path, String api) {
        return  path.equalsIgnoreCase(api) ||
                path.equalsIgnoreCase(api+ "/");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        Map<String,String> parameters = new HashMap<>();
        if(isPath(path,"/api/global/shutdown")) {
            respond(exchange,handler.terminate(),200);
        }else if(isPath(path,"/api/protocols")){
            respond(exchange,handler.getProtocols(),200);
        }else if(isPath(path,"/api/protocols/{protocolInstanceId}/filters",parameters)){
            respond(exchange,handler.getProtocolFilters(parameters.get("protocolInstanceId")),200);
        }else if(isPath(path,"/api/protocols/{protocolInstanceId}/filters/{filterId}/{action}",parameters)){
            respond(exchange,handler.handleProtocolFilterActivation(
                    parameters.get("protocolInstanceId"),
                    parameters.get("filterId"),
                    parameters.get("action")),200);
        }
    }

    private void respond(HttpExchange exchange, Object toSend,int errorCode) {
        try {
            String response = mapper.serialize(toSend);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(errorCode, response.length());
            var os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.flush();
            os.close();
        }catch (Exception e){
            try {
                var result = new Ko();
                result.setError(e.getMessage());
                String response = mapper.serialize(result);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, response.length());
                var os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.flush();
                os.close();
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
    }
}
