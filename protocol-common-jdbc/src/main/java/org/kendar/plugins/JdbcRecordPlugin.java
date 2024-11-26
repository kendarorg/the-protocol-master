package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.WriteItemEvent;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
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

import java.util.List;

public abstract class JdbcRecordPlugin extends RecordPlugin<BasicRecordPluginSettings> {


    @Override
    protected void postCall(PluginContext pluginContext, Object obIn, Object obOUt) {
        JdbcCall in = (JdbcCall) obIn;
        SelectResult out = (SelectResult) obOUt;
        var duration = System.currentTimeMillis() - pluginContext.getStart();
        var req = new JdbcRequest(in.getQuery(), in.getParameterValues());
        JdbcResponse res;
        if (!out.isIntResult()) {
            res = new JdbcResponse(out);
        } else {
            res = new JdbcResponse(out.getCount());
            res.setSelectResult(out);
        }
        var id = (long) pluginContext.getTags().get("id");


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
        if (!shouldNotSaveJdbc(storageItem, compactLine) || !shouldIgnoreTrivialCalls()) {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
        } else {
            EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), compactLine, id)));
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")

    protected boolean shouldNotSaveJdbc(StorageItem in, CompactLine out) {
        var result = in.retrieveInAs(JdbcRequest.class);
        return result.getQuery().trim().toLowerCase().startsWith("set");
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL);
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        withStorage((StorageRepository) global.getService("storage"));
        super.initialize(global, protocol, pluginSetting);
        return this;
    }

    @Override
    protected void handleActivation(boolean active) {
        if (this.isActive() != active) {
            this.storage.isRecording(getInstanceId(), active);
        }
        if (!active) {
            terminate();
        }
    }
}
