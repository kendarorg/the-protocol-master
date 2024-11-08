package org.kendar.filters;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.filters.settings.BasicRecordingPluginSettings;
import org.kendar.proxy.FilterContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.Map;

public abstract class BasicRecordingPlugin<T,K> extends ProtocolPluginDescriptor<Object, Object>{
    protected static JsonMapper mapper = new JsonMapper();
    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, Object in, Object out) {
        if(shouldNotSave(in,out)){
            return false;
        }
        if(phase==ProtocolPhase.POST_CALL) {
            postCall(filterContext, in, out);
        }else if(phase==ProtocolPhase.ASYNC_RESPONSE){
            asyncCall(filterContext,out);
        }
        return false;
    }

    protected void asyncCall(FilterContext filterContext, Object out) {
        var duration = 0;

        var res = "{\"type\":\"" + out.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(out)) + "}";
        var req = "{\"type\":null,\"data\":null}";

        var storageItem = new StorageItem(filterContext.getContext().getContextId(),
                mapper.toJsonNode(req),
                mapper.toJsonNode(res),
                duration, "RESPONSE",
                filterContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
    }

    protected void postCall(FilterContext filterContext, Object in, Object out) {
        var duration = System.currentTimeMillis() - filterContext.getStart();

        var req = "{\"type\":\"" + in.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(in)) + "}";
        var res = "{\"type\":null,\"data\":null}";

        if (out != null) {
            res = "{\"type\":\"" + out.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(out)) + "}";
        }
        var storageItem = new StorageItem(filterContext.getContext().getContextId(),
                mapper.toJsonNode(req),
                mapper.toJsonNode(res),
                duration, filterContext.getType(),
                filterContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
    }

    public Map<String, String> buildTag(StorageItem<JsonNode, JsonNode> item) {
        return Map.of();
    }

    protected boolean shouldNotSave(Object in, Object out){
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL,ProtocolPhase.ASYNC_RESPONSE);
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        return this;
    }

    @Override
    public void terminate() {

    }

    @Override
    public String getId() {
        return "recording-plugin";
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicRecordingPluginSettings.class;
    }

    protected Object getData(Object of) {
        return of;
    }
}
