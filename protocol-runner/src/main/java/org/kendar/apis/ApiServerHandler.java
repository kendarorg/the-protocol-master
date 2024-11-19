package org.kendar.apis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.kendar.plugins.BaseApiServerHandler;
import org.kendar.plugins.apis.FileDownload;
import org.kendar.plugins.apis.Ko;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApiServerHandler implements HttpHandler, BaseApiServerHandler {
    private static final JsonMapper mapper = new JsonMapper();
    private final ApiHandler handler;
    private static final Logger log = LoggerFactory.getLogger(ApiServerHandler.class);

    public ApiServerHandler(ApiHandler handler) {

        this.handler = handler;
    }

    public boolean isPartialPath(String path, String api) {
        return (path.startsWith(api) ||
                path.startsWith(api + "/"));
    }

    public boolean isPath(String path, String api, Map<String, String> parameters) {
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
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    public boolean isPath(String path, String api) {
        return path.equalsIgnoreCase(api) ||
                path.equalsIgnoreCase(api + "/");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        Map<String, String> parameters = new HashMap<>();
        for(var instance: handler.getInstances()) {
            for (var plugin : instance.getPlugins()) {
                var handler = plugin.getApiHandler();
                var rootPath = "/api/protocols/"+handler.getProtocolInstanceId()+"/plugins/"+handler.getId();
                var wildcardProtocolPath = "/api/protocols/*/plugins/"+handler.getId();
                if(isPartialPath(path,rootPath)){
                    if(handler.handle(this,exchange,path.replace(rootPath,""))){
                        return;
                    }
                }else if(isPartialPath(path,wildcardProtocolPath)){
                    if(handler.handle(this,exchange,path.replace(wildcardProtocolPath,""))){
                        return;
                    }
                }
            }
        }
        if (isPath(path, "/api/global/shutdown")) {
            respond(exchange, handler.terminate(), 200);
            return;
        } else if (isPath(path, "/api/protocols")) {
            respond(exchange, handler.getProtocols(), 200);
            return;
        }  else if (isPath(path, "/api/storage/{action}",parameters)) {
            respond(exchange, handler.handleStorage(parameters.get("action"),exchange), 200);
            return;
        } else if (isPath(path, "/api/protocols/{protocolInstanceId}/plugins", parameters)) {
            respond(exchange, handler.getProtocolPlugins(parameters.get("protocolInstanceId")), 200);
            return;
        }
        respond(exchange, new Ko("Not found"), 404);
    }



    public void respond(HttpExchange exchange, Object toSend, int errorCode) {
        try {
            var os = exchange.getResponseBody();
            if(toSend instanceof FileDownload){
                exchange.getResponseHeaders().add("Content-Type", ((FileDownload)toSend).getContentType());
                exchange.getResponseHeaders().add("Content-Transfer-Encoding", "binary");
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\""+((FileDownload)toSend).getFileName()+"\";");
                exchange.sendResponseHeaders(errorCode, ((FileDownload)toSend).getData().length);
                os.write(((FileDownload)toSend).getData());
            }else {
                String response = mapper.serialize(toSend);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(errorCode, response.length());
                os.write(response.getBytes());
            }
            os.flush();
            os.close();
        } catch (Exception e) {
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
            } catch (Exception e2) {
                log.warn(e.getMessage(), e);
            }
        }
    }
}
