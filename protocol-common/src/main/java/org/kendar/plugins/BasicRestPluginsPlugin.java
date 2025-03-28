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
import org.kendar.plugins.dtos.RestPluginsInterceptor;
import org.kendar.plugins.settings.BasicRestPluginsPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.ExtraBeanUtils;
import org.kendar.utils.JsonMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BasicRestPluginsPlugin extends ProtocolPluginDescriptorBase<BasicRestPluginsPluginSettings> {
    public BasicRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    private ConcurrentHashMap<ProtocolPhase, Map<String,List<RestPluginsInterceptor>>> interceptors = new ConcurrentHashMap<>();
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
        for(var item:getSettings().getInterceptors()){
            if(!interceptors.containsKey(item.getPhase())){
                interceptors.put(item.getPhase(),new HashMap<>());
            }
            var interceptorForPhase = interceptors.get(item.getPhase());
            var key = item.getInputType()+"."+item.getOutputType();
            if(!interceptorForPhase.containsKey(key)){
                interceptorForPhase.put(key,new ArrayList<>());
            }
            var interceptorForMessage = interceptorForPhase.get(key);
            interceptorForMessage.add(new RestPluginsInterceptor(item));
        }
    }


    @Override
    public String getId() {
        return "rest-plugins-plugin";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if(!interceptors.containsKey(phase)){
                return false;
            }
            var possibleInterceptors = interceptors.get(phase);
            var inMatch = in==null?"Object":in.getClass().getSimpleName();
            var outMatch = in==null?"Object":out.getClass().getSimpleName();
            var key = inMatch+"."+outMatch;
            if(!possibleInterceptors.containsKey(key)){
                return false;
            }
            var inSerialized = mapper.serialize(in);
            var outSerialized = mapper.serialize(out);
            for(var interceptor:possibleInterceptors.get(key)){
                RestPluginsCallResult result = callInterceptor(interceptor,inSerialized,outSerialized);
                if(result.isBlocking()){
                    if(out!=null){
                        var toReturn = mapper.deserialize(result.getMessage(),out.getClass());
                        try {
                            ExtraBeanUtils.copyProperties(out,toReturn);
                        } catch (Exception e) {
                            throw new PluginException("Unable to copy properties of "+out.getClass().getSimpleName(),e);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private RestPluginsCallResult callInterceptor(RestPluginsInterceptor interceptor, String inSerialized, String outSerialized) {
        try(var httpclient = HttpClients.createDefault()){
            var restPluginCall = new RestPluginCall(interceptor,inSerialized,outSerialized);
            var httpget = new HttpPost(interceptor.getTarget());
            var be = new ByteArrayEntity(mapper.serialize(restPluginCall).getBytes());
            httpget.setEntity(be);
            httpget.setHeader("Content-Type", "application/json");
            var httpresponse = httpclient.execute(httpget);

            var sc = new Scanner(httpresponse.getEntity().getContent());
            var result = "";
            while (sc.hasNext()) {
                result += (sc.nextLine());
            }
            return mapper.deserialize(result, RestPluginsCallResult.class);

        }catch (Exception e){
            throw new PluginException("Unable to call interceptor "+interceptor.getTarget(),e);
        }
    }
}
