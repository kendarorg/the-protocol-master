package org.kendar.filters;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.pf4j.Extension;

import java.util.List;
import java.util.Map;

@Extension
public class HttpFilter extends ProtocolPluginDescriptor<Request, Response> implements AlwaysActivePlugin{
    @Override
    public boolean handle(ProtocolPhase phase, Request in, Response out) {
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "http-filter";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public void initialize(Map<String, Object> section) {

    }

    @Override
    public void terminate() {

    }
}
