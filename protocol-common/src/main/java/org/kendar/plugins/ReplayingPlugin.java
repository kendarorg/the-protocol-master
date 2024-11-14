package org.kendar.plugins;

import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class ReplayingPlugin extends ProtocolPluginDescriptor<Object, Object> {
    protected static final JsonMapper mapper = new JsonMapper();
    protected final HashSet<Integer> completedIndexes = new HashSet<>();
    protected final HashSet<Integer> completedOutIndexes = new HashSet<>();
    protected StorageRepository storage;

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        super.initialize(global, protocol);
        withStorage((StorageRepository) global.getService("storage"));
        return this;
    }

    public ReplayingPlugin withStorage(StorageRepository storage) {

        if (storage != null) {
            this.storage = storage;
        }
        return this;
    }

    protected boolean hasCallbacks() {
        return false;
    }

    protected Object getData(Object of) {
        return of;
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (out == null) {
                sendAndForget(pluginContext, in);
                return true;
            } else {
                sendAndExpect(pluginContext, in, out);
                return true;
            }
        }
        return false;
    }

    protected void sendAndExpect(PluginContext pluginContext, Object in, Object out) {
        var query = new CallItemsQuery();
        var context = pluginContext.getContext();

        query.setCaller(pluginContext.getCaller());
        query.setType(pluginContext.getType());
        query.setUsed(completedIndexes);
        var lineToRead = storage.read(getInstanceId(), query);
        if (lineToRead == null) {
            return;
        }


        var item = lineToRead.getStorageItem();

        if (hasCallbacks()) {
            var afterIndex = item.getIndex();
            var respQuery = new ResponseItemQuery();
            respQuery.setCaller(pluginContext.getCaller());
            respQuery.setUsed(completedOutIndexes);
            respQuery.setStartAt(afterIndex);
            var responses = storage.readResponses(getInstanceId(), respQuery);
            var result = new ArrayList<StorageItem>();
            for (var response : responses) {
                completedOutIndexes.add((int) response.getIndex());
                result.add(response);
            }
            sendBackResponses(pluginContext.getContext(), result);
        }
        var outputItem = item.getOutput();
        if (context.isUseCallDurationTimes()) {
            Sleeper.sleep(item.getDurationMs());
        }
        buildState(pluginContext, context, in, outputItem, out);
    }

    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outputItem, Object aClass) {

    }

    protected void sendAndForget(PluginContext pluginContext, Object in) {
        var query = new CallItemsQuery();

        query.setCaller(pluginContext.getCaller());
        query.setType(in.getClass().getSimpleName());
        query.setUsed(completedIndexes);
        var lineToRead = storage.read(getInstanceId(), query);
        var item = lineToRead.getStorageItem();
        if (hasCallbacks()) {

            var afterIndex = item.getIndex();
            var respQuery = new ResponseItemQuery();
            respQuery.setCaller(pluginContext.getCaller());
            respQuery.setUsed(completedOutIndexes);
            respQuery.setStartAt(afterIndex);
            var responses = storage.readResponses(getInstanceId(), respQuery);
            var result = new ArrayList<StorageItem>();
            for (var response : responses) {
                completedOutIndexes.add((int) response.getIndex());
                result.add(response);
            }
            sendBackResponses(pluginContext.getContext(), result);
        }
    }

    protected abstract void sendBackResponses(ProtoContext context, List<StorageItem> result);

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "replay-plugin";
    }

    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return BasicReplayPluginSettings.class;
    }
}
