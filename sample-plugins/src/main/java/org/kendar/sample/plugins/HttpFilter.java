package org.kendar.sample.plugins;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.*;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

import java.util.Base64;
import java.util.List;

@Extension
@TpmService(tags = "http")
public class HttpFilter extends ProtocolPluginDescriptorBase<HttpFilterSettings> implements AlwaysActivePlugin {
    public HttpFilter(JsonMapper mapper) {

        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        var ct = out.getFirstHeader("content-type");
        if(ct==null)return false;
        if(ct.contains("application/vnd.apple.mpegurl")){
            var result = new String(Base64.getDecoder().decode(out.getResponseText().asText()));
            var lines = result.split("\n");
            System.out.println("result");
        }
        return false;
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpFilterSettings.class;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL);
    }

    @Override
    public String getId() {
        return "sample-http";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    protected ProtocolPluginApiHandler buildApiHandler() {
        return new ProtocolPluginApiHandlerDefault<>(this, getId(), getInstanceId());
    }


}
