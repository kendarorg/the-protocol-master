package org.kendar.http.plugins;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.kendar.filters.FilterDescriptor;
import org.kendar.filters.ProtocolFilterDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.exit;

public class GlobalFilter extends ProtocolFilterDescriptor<Request, Response> {
    private static final Logger log = LoggerFactory.getLogger(GlobalFilter.class);
    private String specialApisRoot;
    private List<FilterDescriptor> filters;
    private HttpServer httpServer;
    private HttpsServer httpsServer;
    private AtomicBoolean shutdownVariable;

    @Override
    public List<org.kendar.filters.ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_RENDER);
    }

    @Override
    public String getId() {
        return "http";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public void initialize(Map<String, Object> section) {

        this.specialApisRoot = section.get("specialApisRoot").toString();


    }

    @Override
    public boolean handle(ProtocolPhase phase, Request request, Response response) {

        if (isPath(request, "api/shutdown")) {
            shutdownVariable.set(true);
            log.info("SHUTDOWN");
            return true;
        } else if (isPath(request, "api/certificates/der")) {
            throw new RuntimeException("Missing der");
        } else if (isPath(request, "api/certificates/key")) {
            throw new RuntimeException("Missing key");
        }
        return false;
    }

    private boolean isPath(Request request, String api) {
        return request.getPath().equalsIgnoreCase("/" + specialApisRoot + "/" + api) ||
                request.getPath().equalsIgnoreCase("/" + specialApisRoot + "/" + api + "/");
    }

    @Override
    public void terminate() {
        for (var filter : filters) {
            if (filter != this) {
                filter.terminate();
            }
            httpServer.stop(0);
            httpsServer.stop(0);
            exit(0);
        }
    }

    public void setFilters(List<FilterDescriptor> filters) {
        this.filters = filters;
    }

    public void setServer(HttpServer server, HttpsServer httpsServer) {
        this.httpServer = server;
        this.httpsServer = httpsServer;
    }

    public void setShutdownVariable(AtomicBoolean shutdownVariable) {
        this.shutdownVariable = shutdownVariable;
    }

}
