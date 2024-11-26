package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.Sleeper;

import java.util.List;
import java.util.Random;

public class HttpLatencyPlugin extends ProtocolPluginDescriptor<Request, Response, HttpLatencyPluginSettings> {


    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (isActive()) {
            HttpLatencyPluginSettings s = getSettings();
            Random r = new Random();
            int waitMs = r.nextInt(s.getMaxMs() - s.getMinMs()) + s.getMinMs();
            if (waitMs > 0) {
                Sleeper.sleep(waitMs);
            }
        }
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "latency-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public void terminate() {

    }
}
