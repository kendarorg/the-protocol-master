package org.kendar.plugins;

import com.sun.net.httpserver.HttpExchange;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Locale;

public class DefaultPluginApiHandler<T extends PluginDescriptor> implements PluginApiHandler {
    protected static final JsonMapper mapper = new JsonMapper();
    private final T descriptor;
    private final String id;
    private final String instanceId;

    public DefaultPluginApiHandler(T descriptor, String id, String instanceId) {
        this.descriptor = descriptor;
        this.id = id;
        this.instanceId = instanceId;
    }

    public T getDescriptor() {
        return descriptor;
    }

    public String getId() {
        return id;
    }

    public String getProtocolInstanceId() {
        return instanceId;
    }

    public boolean handle(BaseApiServerHandler apiServerHandler, HttpExchange exchange, String pathPart) {
        var parameters = new HashMap<String, String>();
        if (apiServerHandler.isPath(pathPart, "/{action}", parameters)) {
            var action = parameters.get("action").toLowerCase(Locale.ROOT);
            switch (action) {
                case "start":
                    descriptor.setActive(true);
                    apiServerHandler.respond(exchange, new Ok(), 200);
                    return true;
                case "stop":
                    descriptor.setActive(false);
                    apiServerHandler.respond(exchange, new Ok(), 200);
                    return true;
                case "status":
                    var status = new Status();
                    status.setActive(descriptor.isActive());
                    apiServerHandler.respond(exchange, status, 200);
                    return true;
                default:
                    apiServerHandler.respond(exchange, new Ko("Unknown action " + action), 404);
                    return true;
            }
        }
        return false;
    }

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
