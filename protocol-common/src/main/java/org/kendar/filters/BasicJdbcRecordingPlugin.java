package org.kendar.filters;

import org.kendar.filters.settings.BasicRecordingPluginSettings;
import org.kendar.proxy.FilterContext;
import org.kendar.settings.GlobalSettings;
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

public abstract class BasicJdbcRecordingPlugin extends ProtocolPluginDescriptor<JdbcCall, SelectResult> {
    protected static JsonMapper mapper = new JsonMapper();
    protected StorageRepository storage;

    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, JdbcCall in, SelectResult out) {
        if (isActive()) {
            postCall(filterContext, in, out);
        }
        return false;
    }


    protected void postCall(FilterContext filterContext, JdbcCall in, SelectResult out) {
        var duration = System.currentTimeMillis() - filterContext.getStart();
        var req = new JdbcRequest(in.getQuery(), in.getParameterValues());
        JdbcResponse res;
        if (!out.isIntResult()) {
            res = new JdbcResponse(out);
        } else {
            res = new JdbcResponse(out.getCount());
        }


        var storageItem = new StorageItem(
                filterContext.getContext().getContextId(),
                req,
                res,
                duration,
                filterContext.getType(),
                filterContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        if (!shouldNotSave(storageItem, compactLine)) {
            storage.write(new LineToWrite(getInstanceId(), storageItem, compactLine));
        } else {
            storage.write(new LineToWrite(getInstanceId(), compactLine));
        }
    }

    public Map<String, String> buildTag(StorageItem item) {
        return Map.of();
    }

    protected boolean shouldNotSave(StorageItem in, CompactLine out) {
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
        storage.finalizeWrite(getInstanceId());
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
