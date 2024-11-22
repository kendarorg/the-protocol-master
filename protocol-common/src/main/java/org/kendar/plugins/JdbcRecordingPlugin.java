package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.FinalizeWriteEvent;
import org.kendar.events.WriteItemEvent;
import org.kendar.plugins.settings.BasicRecordingPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.Map;

public abstract class JdbcRecordingPlugin extends ProtocolPluginDescriptor<JdbcCall, SelectResult> {
    protected static JsonMapper mapper = new JsonMapper();
    private boolean ignoreTrivialCalls = true;
    private StorageRepository storage;

    public boolean shouldIgnoreTrivialCalls() {
        return ignoreTrivialCalls;
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, JdbcCall in, SelectResult out) {
        if (isActive()) {
            if(phase==ProtocolPhase.PRE_CALL){
                pluginContext.getTags().put("id",storage.generateIndex());
            }else if(phase==ProtocolPhase.POST_CALL){
                postCall(pluginContext, in, out);
            }
        }
        return false;
    }

    @Override
    public PluginDescriptor setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        ignoreTrivialCalls = ((BasicRecordingPluginSettings) plugin).isIgnoreTrivialCalls();
        return this;
    }


    protected void postCall(PluginContext pluginContext, JdbcCall in, SelectResult out) {
        var duration = System.currentTimeMillis() - pluginContext.getStart();
        var req = new JdbcRequest(in.getQuery(), in.getParameterValues());
        JdbcResponse res;
        if (!out.isIntResult()) {
            res = new JdbcResponse(out);
        } else {
            res = new JdbcResponse(out.getCount());
            res.setSelectResult(out);
        }
        var id = (long)pluginContext.getTags().get("id");


        var storageItem = new StorageItem(
                pluginContext.getContextId(),
                req,
                res,
                duration,
                pluginContext.getType(),
                pluginContext.getCaller(),
                "JdbcCall", "SelectResult");
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        if (!shouldNotSave(storageItem, compactLine) || !shouldIgnoreTrivialCalls()) {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
        } else {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), compactLine, id)));
        }
    }

    public Map<String, String> buildTag(StorageItem item) {
        return Map.of();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean shouldNotSave(StorageItem in, CompactLine out) {
        var result = in.retrieveInAs(JdbcRequest.class);
        return result.getQuery().trim().toLowerCase().startsWith("set");
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL);
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        super.initialize(global, protocol);
        withStorage((StorageRepository) global.getService("storage"));
        return this;
    }

    public JdbcRecordingPlugin withStorage(StorageRepository storage) {
        if (storage != null) {
            this.storage = storage;
        }
        return this;
    }

    @Override
    public void terminate() {
        EventsQueue.send(new FinalizeWriteEvent(getInstanceId()));
    }

    @Override
    protected void handleActivation(boolean active) {
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
