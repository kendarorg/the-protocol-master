package org.kendar.plugins;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.kendar.exceptions.PluginException;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.dtos.RestPluginCall;
import org.kendar.plugins.dtos.RestPluginsCallResult;
import org.kendar.plugins.settings.BasicRestPluginsPluginSettings;
import org.kendar.plugins.settings.dtos.RestPluginsInterceptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.ExtraBeanUtils;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BasicRestPluginsPlugin extends ProtocolPluginDescriptorBase<BasicRestPluginsPluginSettings> {
    private static final Logger log = LoggerFactory.getLogger(BasicRestPluginsPlugin.class);
    private final ConcurrentHashMap<ProtocolPhase, Map<String, List<org.kendar.plugins.settings.dtos.RestPluginsInterceptor>>> interceptors = new ConcurrentHashMap<>();

    public BasicRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicRestPluginsPluginSettings.class;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL,
                ProtocolPhase.POST_CALL,
                ProtocolPhase.ASYNC_RESPONSE,
                ProtocolPhase.FINALIZE,
                ProtocolPhase.PRE_SOCKET_WRITE,
                ProtocolPhase.CONNECT);
    }

    @Override
    protected boolean handleSettingsChanged() {
        reloadSettings();
        return getSettings() != null;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        handleSettingsChanged();
        return this;
    }

    private void reloadSettings() {
        for (var item : getSettings().getInterceptors()) {
            if (!interceptors.containsKey(item.getPhase())) {
                interceptors.put(item.getPhase(), new HashMap<>());
            }
            var interceptorForPhase = interceptors.get(item.getPhase());
            var key = item.getInputType() + "." + item.getOutputType();
            if (!interceptorForPhase.containsKey(key)) {
                interceptorForPhase.put(key, new ArrayList<>());
            }
            var interceptorForMessage = interceptorForPhase.get(key);
            interceptorForMessage.add(item);
        }
    }

    @Override
    public String getId() {
        return "rest-plugins-plugin";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (!interceptors.containsKey(phase)) {
                return false;
            }
            var possibleInterceptors = interceptors.get(phase);
            var inMatch = in == null ? "Object" : in.getClass().getSimpleName();
            var outMatch = out == null ? "Object" : out.getClass().getSimpleName();
            var key = inMatch + "." + outMatch;
            if (!possibleInterceptors.containsKey(key)) {
                return false;
            }
            var inSerialized = mapper.serialize(in);
            var outSerialized = mapper.serialize(out);

            for (var interceptor : possibleInterceptors.get(key)) {
                if (!interceptor.matches(inSerialized, outSerialized)) {
                    continue;
                }
                try {
                    RestPluginsCallResult result = callInterceptor(interceptor, inSerialized, outSerialized);
                    if (result.isWithError()) {
                        if (result.getError() != null) {
                            throw new PluginException(result.getError());
                        } else {
                            throw new PluginException("Error calling interceptor " + interceptor.getDestinationAddress());
                        }

                    }
                    if (out != null && result.getMessage() != null && !result.getMessage().isEmpty()) {
                        var toReturn = mapper.deserialize(result.getMessage(), out.getClass());
                        try {
                            ExtraBeanUtils.copyProperties(out, toReturn);
                        } catch (Exception e) {
                            log.error("Unable to copy properties", e);
                            throw new PluginException("Unable to copy properties of " + out.getClass().getSimpleName(), e);
                        }
                    }
                    return result.isBlocking();
                } catch (Exception e) {
                    log.error("Unable to execute interceptor", e);
                    if (interceptor.isBlockOnException()) {
                        throw new PluginException("Unable to execute interceptor", e);
                    }
                }
            }
        }
        return false;
    }

    private RestPluginsCallResult callInterceptor(RestPluginsInterceptor interceptor, String inSerialized, String outSerialized) {
        try (var httpclient = HttpClients.createDefault()) {
            var restPluginCall = new RestPluginCall(interceptor, inSerialized, outSerialized);
            var httpPost = new HttpPost(interceptor.getDestinationAddress());
            var be = new ByteArrayEntity(mapper.serialize(restPluginCall).getBytes());
            httpPost.setEntity(be);
            httpPost.setHeader("Content-Type", "application/json");
            var httpResponse = httpclient.execute(httpPost);

            var sc = new Scanner(httpResponse.getEntity().getContent());
            StringBuilder result = new StringBuilder();
            while (sc.hasNext()) {
                result.append(sc.nextLine());
            }
            var toReturn = mapper.deserialize(result.toString(), RestPluginsCallResult.class);
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                toReturn.setWithError(true);
            }
            return toReturn;

        } catch (Exception e) {
            throw new PluginException("Unable to call interceptor " + interceptor.getDestinationAddress(), e);
        }
    }

}
