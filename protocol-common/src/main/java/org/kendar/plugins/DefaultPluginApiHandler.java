package org.kendar.plugins;

import com.sun.net.httpserver.HttpExchange;
import org.kendar.plugins.apis.Ko;
import org.kendar.utils.JsonMapper;

public class DefaultPluginApiHandler<T extends PluginDescriptor> implements PluginApiHandler {
    private final T descriptor;
    private final String id;
    private final String instanceId;

    public T getDescriptor() {
        return descriptor;
    }

    public String getId() {
        return id;
    }

    public String getProtocolInstanceId() {
        return instanceId;
    }

    public DefaultPluginApiHandler(T descriptor, String id, String instanceId) {
        this.descriptor = descriptor;
        this.id = id;
        this.instanceId = instanceId;
    }

    public boolean handle(HttpExchange exchange, String pathPart){
        return false;
    }

    private static JsonMapper mapper = new JsonMapper();

    private void respond(HttpExchange exchange, Object toSend, int errorCode) {
        try {
            String response = mapper.serialize(toSend);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(errorCode, response.length());
            var os = exchange.getResponseBody();
            os.write(response.getBytes());
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
                throw new RuntimeException();
            }
        }
    }
}
