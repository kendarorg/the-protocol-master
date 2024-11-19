package org.kendar.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.events.EventsQueue;
import org.kendar.events.FinalizeWriteEvent;
import org.kendar.events.RecordStatusEvent;
import org.kendar.events.WriteItemEvent;
import org.kendar.plugins.settings.BasicRecordingPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.Map;

public abstract class RecordingPlugin extends ProtocolPluginDescriptor<Object, Object> {
    protected static final JsonMapper mapper = new JsonMapper();
    private boolean ignoreTrivialCalls = true;

    public boolean shouldIgnoreTrivialCalls() {
        return ignoreTrivialCalls;
    }

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

    @Override
    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        ignoreTrivialCalls = ((BasicRecordingPluginSettings) plugin).isIgnoreTrivialCalls();
    }

    protected void asyncCall(PluginContext pluginContext, Object out) {
        var duration = 0;


        var storageItem = new StorageItem(
                pluginContext.getContextId(),
                null,
                mapper.toJsonNode(getData(out)),
                duration, "RESPONSE",
                pluginContext.getCaller(),
                null,
                out.getClass().getSimpleName());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);

        EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine)));
    }

    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var duration = System.currentTimeMillis() - pluginContext.getStart();

        JsonNode resSerialized = null;
        String resType = null;

        if (out != null) {
            resType = out.getClass().getSimpleName();
            resSerialized = mapper.toJsonNode(getData(out));
        }
        var storageItem = new StorageItem(
                pluginContext.getContextId(),
                mapper.toJsonNode(getData(in)),
                resSerialized,
                duration,
                pluginContext.getType(),
                pluginContext.getCaller(),
                in.getClass().getSimpleName(),
                resType);
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        if (!shouldNotSave(in, out, compactLine) || !shouldIgnoreTrivialCalls()) {
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

    @Override
    protected void handleActivation(boolean active) {
        EventsQueue.send(new RecordStatusEvent(active, getProtocol(), getId(), getInstanceId()));
        if (!active) {
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
