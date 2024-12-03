package org.kendar.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.events.*;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class RecordPlugin<W extends BasicRecordPluginSettings> extends ProtocolPluginDescriptorBase< W> {
    protected static final JsonMapper mapper = new JsonMapper();
    protected StorageRepository storage;
    private boolean ignoreTrivialCalls = true;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldIgnoreTrivialCalls() {
        return ignoreTrivialCalls;
    }


    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            switch (phase) {
                case PRE_CALL:
                    pluginContext.getTags().put("id", storage.generateIndex());
                    break;
                case POST_CALL:
                    postCall(pluginContext, in, out);
                    break;
                case ASYNC_RESPONSE:
                    pluginContext.getTags().put("id", storage.generateIndex());
                    asyncCall(pluginContext, out);
                    break;
            }
        }
        return false;
    }

    protected void asyncCall(PluginContext pluginContext, Object out) {
        var duration = 0;

        var id = (long) pluginContext.getTags().get("id");
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

        EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
    }

    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var duration = System.currentTimeMillis() - pluginContext.getStart();

        var id = (long) pluginContext.getTags().get("id");
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
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
        } else {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), compactLine, id)));
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
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL, ProtocolPhase.ASYNC_RESPONSE);
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        withStorage(global.getService("storage"));
        ignoreTrivialCalls = ((BasicRecordPluginSettings) pluginSetting).isIgnoreTrivialCalls();
        super.initialize(global, protocol, pluginSetting);
        return this;
    }

    protected RecordPlugin withStorage(StorageRepository storage) {
        if (storage != null) {
            this.storage = storage;
        }
        return this;
    }

    @Override
    public void terminate() {
        EventsQueue.send(new FinalizeWriteEvent(getInstanceId()));
    }
    private static final Logger log = LoggerFactory.getLogger(RecordPlugin.class);

    @Override
    protected void handleActivation(boolean active) {
        EventsQueue.send(new RecordStatusEvent(active, getProtocol(), getId(), getInstanceId()));
        if (isActive()!=active && !active) {
            terminate();
        }else if (isActive()!=active && active) {
            EventsQueue.send(new StartWriteEvent(getInstanceId()));
        }
    }

    @Override
    public String getId() {
        return "record-plugin";
    }


    protected Object getData(Object of) {
        return of;
    }

}
