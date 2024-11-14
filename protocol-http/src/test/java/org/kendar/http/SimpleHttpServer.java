package org.kendar.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.fileupload.FileUploadException;
import org.kendar.http.utils.converters.RequestResponseBuilderImpl;
import org.kendar.server.KendarHttpServer;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class SimpleHttpServer {
    final JsonMapper mapper = new JsonMapper();
    private RequestResponseBuilderImpl reqResBuilder;
    private HttpServer httpServer;

    public void start(int port) throws IOException {
        var address = new InetSocketAddress(port);
        httpServer = new KendarHttpServer(address, 10);
        var srv = this;
        reqResBuilder = new RequestResponseBuilderImpl();
        httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    srv.handle(exchange);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(1);
    }

    private void handle(HttpExchange exchange) throws IOException, FileUploadException {
        var request = reqResBuilder.fromExchange(exchange, "http");
        var outputStream = exchange.getResponseBody();
        byte[] bytes = null;
        if(request.getPath().endsWith("image.gif")){
            var frf = new FileResourcesUtils();
            bytes = frf.getFileFromResourceAsByteArray("resource://image.gif");
            exchange.getResponseHeaders().add("Content-Type", "image/gif");

        }else if(request.getPath().equalsIgnoreCase("/jsonized")){
            var serializedRequest = mapper.serialize(request);
            bytes = serializedRequest.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(200, bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }
}
