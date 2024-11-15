package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.FinalizeWriteEvent;
import org.kendar.events.WriteItemEvent;
import org.kendar.plugins.settings.BasicRecordingPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.Map;

public abstract class RecordingPlugin extends ProtocolPluginDescriptor<Object, Object> {
    protected static final JsonMapper mapper = new JsonMapper();

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (phase == ProtocolPhase.POST_CALL) {
                postCall(pluginContext, in, out);
            } else if (phase == ProtocolPhase.ASYNC_RESPONSE) {
                asyncCall(pluginContext, out);
            }
        }
        return false;
    }

    protected void asyncCall(PluginContext pluginContext, Object out) {
        var duration = 0;

        var res = "{\"type\":\"" + out.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(out)) + "}";
        var req = "{\"type\":null,\"data\":null}";

        var storageItem = new StorageItem(
                pluginContext.getContextId(),
                mapper.toJsonNode(req),
                mapper.toJsonNode(res),
                duration, "RESPONSE",
                pluginContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);

        EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine)));
    }

    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var duration = System.currentTimeMillis() - pluginContext.getStart();

        var req = "{\"type\":\"" + in.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(in)) + "}";
        var res = "{\"type\":null,\"data\":null}";

        if (out != null) {
            res = "{\"type\":\"" + out.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(out)) + "}";
        }
        var storageItem = new StorageItem(
                pluginContext.getContextId(),
                mapper.toJsonNode(req),
                mapper.toJsonNode(res),
                duration,
                pluginContext.getType(),
                pluginContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        if (!shouldNotSave(in, out, compactLine)) {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine)));
        } else {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), compactLine)));
        }
    }

    public Map<String, String> buildTag(StorageItem item) {
        return Map.of();
    }

    protected boolean shouldNotSave(Object in, Object out, CompactLine compactLine) {
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL, ProtocolPhase.ASYNC_RESPONSE);
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        super.initialize(global, protocol);
        return this;
    }

    @Override
    public void terminate() {
        EventsQueue.send(new FinalizeWriteEvent(getInstanceId()));
    }

    protected void handleActivation(boolean active) {
        if(!active){
            terminate();
        }
    }

    @Override
    public String getId() {
        return "record-plugin";
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicRecordingPluginSettings.class;
    }

    protected Object getData(Object of) {
        return of;
    }
}
