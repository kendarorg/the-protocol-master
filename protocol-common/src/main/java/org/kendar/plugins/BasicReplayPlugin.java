package org.kendar.plugins;

import org.kendar.events.EndPlayEvent;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReplayStatusEvent;
import org.kendar.events.StartPlayEvent;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.PluginFileManager;
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

/**
 * Base class for the plugin
 *
 * @param <W> BasicReplayPluginSettings this should match the getSettingClass method
 */
public abstract class BasicReplayPlugin<W extends BasicReplayPluginSettings> extends ProtocolPluginDescriptorBase<W> {
    private static final Logger log = LoggerFactory.getLogger(BasicReplayPlugin.class);
    /**
     * Container for the completed items
     */
    protected final HashSet<Integer> completedIndexes = new HashSet<>();
    /**
     * Container for the completed responses
     */
    protected final HashSet<Integer> completedOutIndexes = new HashSet<>();
    /**
     * The storage to be used to retrieve the data
     */
    protected final StorageRepository repository;
    /**
     * Indexes locally loaded from the storage. They do not contain the first
     * responses
     */
    private final List<CompactLine> indexes = new ArrayList<>();
    /**
     * Repeatable lines (connections etc)
     */
    private final List<CompactLine> repeatable = new ArrayList<>();
    private PluginFileManager storage;

    public BasicReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper);
        this.repository = storage;
    }

    protected List<String> repeatableItems() {
        return new ArrayList<>();
    }

    /**
     * Retrieve the settings class, must match the <W> parameter
     *
     * @return
     */
    @Override
    public Class<?> getSettingClass() {
        return BasicReplayPluginSettings.class;
    }

    /**
     * Initialize the plugin
     *
     * @param global        The global settings (with storage service)
     * @param protocol      The protocol specific settings
     * @param pluginSetting The plugin settings (must match the getSettingsClass)
     * @return
     */
    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        storage = repository.buildPluginFileManager(getInstanceId(), getId());
        return this;
    }

    /**
     * To override if the protocol has callbacks/subscriptions
     * Default false
     *
     * @return
     */
    protected boolean hasCallbacks() {
        return false;
    }

    /**
     * To do special operations on the data received
     * Usually does nothing
     *
     * @param of
     * @return
     */
    protected Object getData(Object of) {
        return of;
    }

    /**
     * Handle all the messages recorded previously
     *
     * @param pluginContext The execution context
     * @param protocolPhase The current exection phase
     * @param in            The input item. When Object means anything
     * @param out           The output item. When Object means anything
     * @return True when should block further executions
     */
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase,
                          Object in, Object out) {
        if (isActive()) {
            if (out == null) {
                return sendAndForget(pluginContext, in) || getSettings().isBlockExternal();
            } else {
                return sendAndExpect(pluginContext, in, out) || getSettings().isBlockExternal();
            }
        }
        return false;
    }

    /**
     * Operation to initialize the plugin according to activation status
     *
     * @param active
     */
    @Override
    protected void handleActivation(boolean active) {
        try {
            if (this.isActive() != active) {
                getSettings().setActive(active);
                completedOutIndexes.clear();
                completedIndexes.clear();
                if (active) {
                    var repeatableMessageTypes = repeatableItems();
                    EventsQueue.send(new StartPlayEvent(getInstanceId()));
                    Sleeper.sleep(1000, () -> this.repository.getIndexes(getInstanceId()) != null);
                    var toCleanIndexes = new ArrayList<>(this.repository.getIndexes(getInstanceId()));
                    var fromHereTheyAreNotResponses = false;
                    indexes.clear();
                    repeatable.clear();
                    for (var toClean : toCleanIndexes) {
                        if (repeatableMessageTypes.contains(toClean.getType())) {
                            repeatable.add(toClean);
                        }
                        if (fromHereTheyAreNotResponses) {
                            indexes.add(toClean);
                        } else {
                            if ("RESPONSE".equalsIgnoreCase(toClean.getType())) continue;
                            fromHereTheyAreNotResponses = true;
                            indexes.add(toClean);
                        }
                    }
                } else {
                    EventsQueue.send(new EndPlayEvent(getInstanceId()));
                    indexes.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error handling activation", e);
        }

        EventsQueue.send(new ReplayStatusEvent(active, getProtocol(), getId(), getInstanceId()));
    }


    @Override
    protected void handlePostActivation(boolean active) {
        if (getSettings() instanceof BasicAysncReplayPluginSettings) {
            var bas = (BasicAysncReplayPluginSettings) this.getSettings();
            if (bas.isResetConnectionsOnStart()) {
                disconnectAll();
            }
        }
    }

    protected void disconnectAll() {
        var pi = getProtocolInstance();
        if (pi != null && BasicAysncReplayPluginSettings.class.isAssignableFrom(getSettings().getClass())) {
            var settings = (BasicAysncReplayPluginSettings) getSettings();
            if (settings.isResetConnectionsOnStart()) {
                for (var contextKvp : pi.getContextsCache().entrySet()) {
                    try {
                        var context = contextKvp.getValue();
                        var contextConnection = context.getValue("CONNECTION");
                        if (contextConnection != null) {
                            context.disconnect(((ProxyConnection) contextConnection).getConnection());
                            context.setValue("CONNECTION", null);
                        }
                    } catch (Exception e) {
                        log.trace("Error disconnecting {}", contextKvp.getKey(), e);
                    }
                    pi.getContextsCache().remove(contextKvp.getKey());
                }
            }
        }
    }

    /**
     * What to do when the proxy requests a response
     *
     * @param pluginContext
     * @param in
     * @param out
     * @return
     */
    protected boolean sendAndExpect(PluginContext pluginContext, Object in, Object out) {
        var query = new CallItemsQuery();
        var context = pluginContext.getContext();


        query.setCaller(pluginContext.getCaller());
        query.setType(pluginContext.getType());
        query.setUsed(completedIndexes);
        query.getTags().putAll(buildTag(in));
        var index = findIndex(query, in);
        if (repository == null) {
            log.error("Missing storage for context {}-{}", pluginContext.getContext().getContextId(), pluginContext.getCaller());
        }
        if (index == null) {
            log.error("Missing index for query {}", query);
            return false;
        }
        var storageItem = readStorageItem(index, in, pluginContext);
        if (storageItem == null) {
            if (index.getLine().getIndex() == -1 && !getSettings().isBlockExternal()) {
                return false;
            }
            storageItem = new StorageItem();
            storageItem.setIndex(index.getLine().getIndex());
        }

        var lineToRead = new LineToRead(storageItem, index.getLine());
        completedIndexes.add((int) lineToRead.getStorageItem().getIndex());

        var item = lineToRead.getStorageItem();


        var outputItem = item.getOutput();
        if (context.isUseCallDurationTimes()) {
            Sleeper.sleep(item.getDurationMs());
        }
        lineToRead = beforeSendingReadResult(lineToRead);
        buildState(pluginContext, context, in, outputItem, out, lineToRead);
        lineToRead.getStorageItem().setConnectionId(pluginContext.getContextId());

        loadAndPrepareTheAsyncResponses(pluginContext, item, index);

        return true;
    }

    /**
     * Load from index the async responses following the ones returned
     *
     * @param pluginContext
     * @param item
     * @param index
     */
    private void loadAndPrepareTheAsyncResponses(PluginContext pluginContext, StorageItem item, ReplayFindIndexResult index) {
        if (hasCallbacks() && item != null) {
            var afterIndex = item.getIndex();
            var tmpTimestamp = item.getTimestamp();
            if (index != null && index.getLine() != null &&
                    index.getLine().getIndex() != -1 && !index.isRepeated()) {
                tmpTimestamp = index.getLine().getTimestamp();
            }
            var timeStamp = tmpTimestamp;
            var respQuery = new ResponseItemQuery();
            respQuery.setCaller(pluginContext.getCaller());
            respQuery.setUsed(completedOutIndexes);
            respQuery.setStartAt(afterIndex);
            //respQuery.getTags().putAll(getContextTags(pluginContext.getContext()));
            log.debug("[XXX] Request query {}", respQuery);

            var responses = repository.readResponsesFromScenario(getInstanceId(), respQuery);
            responses.sort(Comparator.comparingLong(StorageItem::getTimestamp));
            var result = new ArrayList<StorageItem>();
            for (var response : responses) {
                var idx = indexes.stream().filter(a -> a.getIndex() == response.getIndex()).findFirst();
                if (idx.isPresent()) {
                    for (var contextCached : pluginContext.getContext().getDescriptor().getContextsCache().values()) {
                        var tags = getContextTags(contextCached);
                        if (tagsMatching(idx.get().getTags(), tags) > 0) {
                            completedOutIndexes.add((int) response.getIndex());
                            response.setConnectionId(contextCached.getContextId());
                            result.add(response);
                            break;
                        }
                    }

                }

            }
            if (!result.isEmpty()) {
                ((NetworkProtoContext) pluginContext.getContext()).addResponse(() ->
                {
                    var sleepint = result.get(0).getTimestamp() - timeStamp;
                    if (sleepint > 0) {
                        Sleeper.sleep(sleepint);
                    }
                    sendBackResponses(pluginContext.getContext(), result);
                });
            }
        }
    }

    /**
     * Overridable, retrieve the tags from the context (e.g. in case of a queue
     * it adds to the message the queue to which the message is associated)
     *
     * @param context
     * @return
     */
    protected Map<String, String> getContextTags(ProtoContext context) {
        return Map.of();
    }

    /**
     * Read a storage item to reproduce
     *
     * @param index
     * @param in
     * @param pluginContext
     * @return
     */
    protected StorageItem readStorageItem(ReplayFindIndexResult index, Object in, PluginContext pluginContext) {
        var item = repository.readFromScenarioById(getInstanceId(), index.getLine().getIndex());
        if (item != null && index.isRepeated()) {
            if (!getSettings().isBlockExternal()) {
                return null;
            }
            var result = new StorageItem();
            result.setIndex(item.getIndex());
            result.setTimestamp(item.getTimestamp());
            result.setCaller(item.getCaller());
            result.setConnectionId(pluginContext.getContextId());
            result.setInputType(item.getInputType());
            result.setOutputType(item.getOutputType());
            result.setDurationMs(item.getDurationMs());
            result.setInput(in);
            result.setOutput(mapper.clone(item.getOutput()));
            item = result;
        }
        return item;
    }

    /**
     * Build the tags from an input object
     *
     * @param in
     * @return
     */
    protected Map<String, String> buildTag(Object in) {
        return Map.of();
    }

    /**
     * Operation to modify the message before sending it back to clien
     *
     * @param lineToRead
     * @return
     */
    protected LineToRead beforeSendingReadResult(LineToRead lineToRead) {
        return lineToRead;
    }

    /**
     * Final operations on the
     *
     * @param pluginContext
     * @param context
     * @param in
     * @param outputItem
     * @param aClass
     * @param lineToRead
     */
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outputItem, Object aClass, LineToRead lineToRead) {

    }

    /**
     * HAndle the send without responses
     *
     * @param pluginContext
     * @param in
     * @return
     */
    protected boolean sendAndForget(PluginContext pluginContext, Object in) {
        var query = new CallItemsQuery();

        query.setCaller(pluginContext.getCaller());
        query.setType(in.getClass().getSimpleName());
        query.setUsed(completedIndexes);
        query.getTags().putAll(buildTag(in));
        var index = findIndex(query, in);

        if (index == null) {
            return false;
        }
        var storageItem = readStorageItem(index, in, pluginContext);
        if (storageItem == null) {
            //MAIN_TODO Handle sending when connection exists and
            //Can therefore send the message forward
            storageItem = new StorageItem();
            storageItem.setIndex(index.getLine().getIndex());
        }
        storageItem.setConnectionId(pluginContext.getContextId());

        var lineToRead = new LineToRead(storageItem, index.getLine());
        var item = lineToRead.getStorageItem();
        completedIndexes.add((int) lineToRead.getStorageItem().getIndex());
        loadAndPrepareTheAsyncResponses(pluginContext, item, index);
        return true;
    }

    /**
     * Send back the responses, only for callback enabled protocols
     *
     * @param context
     * @param result
     */
    protected void sendBackResponses(ProtoContext context, List<StorageItem> result) {

    }

    /**
     * The phases for which the plugin is enabled.
     * In this case only on PRE_CALL, intercepting all "real" call from
     * the client
     *
     * @return
     */
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    /**
     * The plugin id
     *
     * @return
     */
    @Override
    public String getId() {
        return "replay-plugin";
    }

    /**
     * Find the correct index given the input object (from which the tags are loaded)
     * and following the query object
     *
     * @param query
     * @param in
     * @return
     */
    protected ReplayFindIndexResult findIndex(CallItemsQuery query, Object in) {
        var idx = indexes.stream()
                .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                .filter(a ->
                        typeMatching(query.getType(), a.getType()) &&
                                a.getCaller().equalsIgnoreCase(query.getCaller())
                ).toList();
        CompactLine bestIndex = null;
        CompactLine bestIndexRepeated = null;
        var maxMatch = -1;
        var maxMatchRepeated = -1;
        //Evaluate the most matching item
        for (var index : idx) {
            var used = query.getUsed().stream().anyMatch((n) -> n == index.getIndex());
            var isRepeatableItem = repeatableItems().contains(index.getType()) && verifyContentRepeatable(index);
            if (isRepeatableItem && !getSettings().isBlockExternal()) {
                return null;
            }
            if (used) {
                if (!getSettings().isIgnoreTrivialCalls()) continue;
                if (isRepeatableItem) {
                    var currentMatch = tagsMatching(index.getTags(), query.getTags());
                    if (currentMatch > maxMatchRepeated) {
                        maxMatchRepeated = currentMatch;
                        bestIndexRepeated = index;
                    }
                    continue;
                }
                continue;
            }
            var currentMatch = tagsMatching(index.getTags(), query.getTags());
            if (currentMatch > maxMatch) {
                maxMatch = currentMatch;
                bestIndex = index;
            }
        }
        if (bestIndex != null) {
            log.debug("Matched for replay: {}.{}", bestIndex.getCaller(), bestIndex.getIndex());
            return new ReplayFindIndexResult(bestIndex, false);
        } else if (bestIndexRepeated != null) {
            log.debug("Repeated match for reply: {}.{} {}", query.getCaller(), query.getType(), query.getTags());
            return new ReplayFindIndexResult(mapper.clone(bestIndexRepeated), true);
        } else {
            log.debug("No match for reply: {}.{} {}", query.getCaller(), query.getType(), query.getTags());
            return null;
        }

    }

    protected boolean verifyContentRepeatable(CompactLine index) {
        return true;
    }

    /**
     * Match the possible type with the real type
     *
     * @param type
     * @param possibleType
     * @return
     */
    private boolean typeMatching(String type, String possibleType) {
        if ("RESPONSE".equalsIgnoreCase(possibleType)) return false;
        if (type == null || type.isEmpty()) return true;
        return type.equalsIgnoreCase(possibleType);
    }

    /**
     * Check for matching tags and return a confidence value
     *
     * @param tags
     * @param query
     * @return
     */
    protected int tagsMatching(Map<String, String> tags, Map<String, String> query) {
        var result = 0;
        for (var tag : query.entrySet()) {
            if (tags.containsKey(tag.getKey())) {
                var l = tags.get(tag.getKey());
                var r = query.get(tag.getKey());
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
