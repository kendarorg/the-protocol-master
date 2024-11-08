package org.kendar.http.plugins;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.kendar.filters.PluginDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.proxy.FilterContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.exit;

public class GlobalPlugin extends ProtocolPluginDescriptor<Request, Response> {
    private static final Logger log = LoggerFactory.getLogger(GlobalPlugin.class);
    private String apis;
    private List<PluginDescriptor> filters;
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
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        var setttings = (HttpProtocolSettings)protocol;
        this.apis = setttings.getApis();
        return this;
    }

    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, Request request, Response response) {

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
        return request.getPath().equalsIgnoreCase("/" + apis + "/" + api) ||
                request.getPath().equalsIgnoreCase("/" + apis + "/" + api + "/");
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

    @Override
    public Class<?> getSettingClass() {
        return null;
    }

    @Override
    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);

    }

    public void setFilters(List<PluginDescriptor> filters) {
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
