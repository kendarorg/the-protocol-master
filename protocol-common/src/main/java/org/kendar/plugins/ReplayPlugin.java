package org.kendar.plugins;

import org.kendar.events.EndPlayEvent;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReplayStatusEvent;
import org.kendar.events.StartPlayEvent;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public abstract class ReplayPlugin<W extends BasicReplayPluginSettings> extends ProtocolPluginDescriptorBase<W> {
    protected static final JsonMapper mapper = new JsonMapper();
    static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Logger log = LoggerFactory.getLogger(ReplayPlugin.class);
    protected final HashSet<Integer> completedIndexes = new HashSet<>();
    protected final HashSet<Integer> completedOutIndexes = new HashSet<>();
    protected StorageRepository storage;
    private List<CompactLine> indexes;

    @Override
    public Class<?> getSettingClass() {
        return BasicReplayPluginSettings.class;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        withStorage(global.getService("storage"));
        super.initialize(global, protocol, pluginSetting);
        return this;
    }

    protected ReplayPlugin withStorage(StorageRepository storage) {
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

    @SuppressWarnings("IfStatementWithIdenticalBranches")

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (out == null) {
                if(!sendAndForget(pluginContext, in) && !getSettings().isBlockExternal()){
                    return false;
                }
                return true;
            } else {
                if(!sendAndExpect(pluginContext, in, out) && !getSettings().isBlockExternal()){
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void handleActivation(boolean active) {
        try {
            if (this.isActive() != active) {
                completedOutIndexes.clear();
                completedIndexes.clear();
                if (active) {
                    EventsQueue.send(new StartPlayEvent(getInstanceId()));
                    Sleeper.sleep(100);
                    indexes = new ArrayList<>(this.storage.getIndexes(getInstanceId()));
                } else {
                    EventsQueue.send(new EndPlayEvent(getInstanceId()));
                    indexes.clear();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        EventsQueue.send(new ReplayStatusEvent(active, getProtocol(), getId(), getInstanceId()));
    }


    protected boolean sendAndExpect(PluginContext pluginContext, Object in, Object out) {
        var query = new CallItemsQuery();
        var context = pluginContext.getContext();

        query.setCaller(pluginContext.getCaller());
        query.setType(pluginContext.getType());
        query.setUsed(completedIndexes);
        query.getTags().putAll(buildTag(in));
        var index = findIndex(query,in);
        if (storage == null) {
            log.error("LOGGER NULL");
        }
        if (index == null) {
            log.error("INDEX NULL {}", query);
            return false;
        }
        var storageItem = readStorageItem(index,in, pluginContext);
        if (storageItem == null) {
            if(index.getIndex()==-1 && !getSettings().isBlockExternal()){
                return false;
            }
            storageItem = new StorageItem();
            storageItem.setIndex(index.getIndex());
        }

        var lineToRead = new LineToRead(storageItem, index);
        completedIndexes.add((int) lineToRead.getStorageItem().getIndex());

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
            if (!result.isEmpty()) {
                executor.submit(() -> sendBackResponses(pluginContext.getContext(), result));
            }
        }
        var outputItem = item.getOutput();
        if (context.isUseCallDurationTimes()) {
            Sleeper.sleep(item.getDurationMs());
        }
        lineToRead = beforeSendingReadResult(lineToRead);
        buildState(pluginContext, context, in, outputItem, out, lineToRead);
        return true;
    }

    protected StorageItem readStorageItem(CompactLine index, Object in, PluginContext pluginContext) {
        return storage.readById(getInstanceId(), index.getIndex());
    }

    protected StorageItem mockResponse(CompactLine index) {
        return null;
    }

    protected Map<String, String> buildTag(Object in) {
        return Map.of();
    }

    protected LineToRead beforeSendingReadResult(LineToRead lineToRead) {
        return lineToRead;
    }

    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outputItem, Object aClass, LineToRead lineToRead) {

    }

    protected boolean sendAndForget(PluginContext pluginContext, Object in) {
        var query = new CallItemsQuery();

        query.setCaller(pluginContext.getCaller());
        query.setType(in.getClass().getSimpleName());
        query.setUsed(completedIndexes);
        query.getTags().putAll(buildTag(in));
        var index = findIndex(query,in);
        if(index == null) {
            return false;
        }
        var storageItem = readStorageItem(index, in,pluginContext);
        if (storageItem == null) {
            storageItem = new StorageItem();
            storageItem.setIndex(index.getIndex());
        }

        var lineToRead = new LineToRead(storageItem, index);
        var item = lineToRead.getStorageItem();

        completedIndexes.add((int) lineToRead.getStorageItem().getIndex());
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
            if (!result.isEmpty()) {
                executor.submit(() -> sendBackResponses(pluginContext.getContext(), result));
            }
        }
        return true;
    }

    protected void sendBackResponses(ProtoContext context, List<StorageItem> result) {

    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "replay-plugin";
    }

    protected CompactLine findIndex(CallItemsQuery query,Object in) {
        var idx = indexes.stream()
                .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                .filter(a ->
                        typeMatching(query.getType(), a.getType()) &&
                                a.getCaller().equalsIgnoreCase(query.getCaller()) &&
                                query.getUsed().stream().noneMatch((n) -> n == a.getIndex())
                ).collect(Collectors.toList());
        CompactLine bestIndex = null;
        var maxMatch = -1;
        for (var index : idx) {
            var currentMatch = tagsMatching(index.getTags(), query);
            if (currentMatch > maxMatch) {
                maxMatch = currentMatch;
                bestIndex = index;
            }
        }
        log.debug("Matched query: {} {}", query.getTag("query"), bestIndex);
        return bestIndex;
    }

    protected int addParametricMatching(Map<String, String> possible, HashMap<String, String> query) {
        return 0;
    }

    private boolean typeMatching(String type, String type1) {
        if ("RESPONSE".equalsIgnoreCase(type1)) return false;
        if (type == null || type.isEmpty()) return true;
        return type.equalsIgnoreCase(type1);
    }

    protected int tagsMatching(Map<String, String> tags, CallItemsQuery query) {
        var result = 0;
        for (var tag : query.getTags().entrySet()) {
            if (tags.containsKey(tag.getKey())) {
                var l = tags.get(tag.getKey());
                var r = query.getTags().get(tag.getKey());
                //noinspection StringEquality
                if ((l == null || r == null) && l == r) {
                    result++;
                } else if (l != null && l.equalsIgnoreCase(r)) {
                    result++;
                }
            }
        }
        return result;
    }
}
