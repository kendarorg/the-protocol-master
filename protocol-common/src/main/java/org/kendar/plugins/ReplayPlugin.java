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
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for the plugin
 *
 * @param <W> BasicReplayPluginSettings this should match the getSettingClass method
 */
public abstract class ReplayPlugin<W extends BasicReplayPluginSettings> extends ProtocolPluginDescriptorBase<W> {
    private static final Logger log = LoggerFactory.getLogger(ReplayPlugin.class);

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
    protected StorageRepository storage;

    /**
     * Indexes locally loaded from the storage. They do not contain the first
     * responses
     */
    private List<CompactLine> indexes = new ArrayList<>();

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
        withStorage(global.getService("storage"));
        super.initialize(global, protocol, pluginSetting);
        return this;
    }

    /**
     * Set the storage on the plugin
     *
     * @param storage
     * @return
     */
    protected ReplayPlugin withStorage(StorageRepository storage) {
        if (storage != null) {
            this.storage = storage;
        }
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

                completedOutIndexes.clear();
                completedIndexes.clear();
                if (active) {
                    EventsQueue.send(new StartPlayEvent(getInstanceId()));
                    Sleeper.sleep(1000, () -> this.storage.getIndexes(getInstanceId()) != null);
                    var toCleanIndexes = new ArrayList<>(this.storage.getIndexes(getInstanceId()));
                    var fromHereTheyAreNotResponses = false;
                    indexes.clear();
                    for (var toClean : toCleanIndexes) {
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
            log.error(e.getMessage(), e);
        }
        EventsQueue.send(new ReplayStatusEvent(active, getProtocol(), getId(), getInstanceId()));
    }


    @Override
    protected void handlePostActivation(boolean active) {
        disconnectAll();
    }

    private void disconnectAll() {
        var pi = getProtocolInstance();
        if (pi != null && BasicAysncReplayPluginSettings.class.isAssignableFrom(getSettings().getClass())) {
            var settings = (BasicAysncReplayPluginSettings) getSettings();
            if (settings.isResetConnectionsOnStart()) {
                for (var contextKvp : pi.getContextsCache().entrySet()) {
                    try {
                        var context = contextKvp.getValue();
                        var contextConnection = context.getValue("CONNECTION");
                        context.disconnect(((ProxyConnection) contextConnection).getConnection());
                        context.setValue("CONNECTION", null);
                    } catch (Exception e) {
                        log.debug("Error disconnecting connection {}", contextKvp.getKey(), e);
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
        if (storage == null) {
            log.error("LOGGER NULL");
        }
        if (index == null) {
            log.error("INDEX NULL {}", query);
            return false;
        }
        var storageItem = readStorageItem(index, in, pluginContext);
        if (storageItem == null) {
            if (index.getIndex() == -1 && !getSettings().isBlockExternal()) {
                return false;
            }
            storageItem = new StorageItem();
            storageItem.setIndex(index.getIndex());
        }

        var lineToRead = new LineToRead(storageItem, index);
        completedIndexes.add((int) lineToRead.getStorageItem().getIndex());

        var item = lineToRead.getStorageItem();


        var outputItem = item.getOutput();
        if (context.isUseCallDurationTimes()) {
            Sleeper.sleep(item.getDurationMs());
        }
        lineToRead = beforeSendingReadResult(lineToRead);
        buildState(pluginContext, context, in, outputItem, out, lineToRead);
        lineToRead.getStorageItem().setConnectionId(pluginContext.getContextId());

        loadAndPrepareTheAsyncResponses(pluginContext, item);

        return true;
    }

    /**
     * Load from index the async responses following the ones returned
     *
     * @param pluginContext
     * @param item
     */
    private void loadAndPrepareTheAsyncResponses(PluginContext pluginContext, StorageItem item) {
        if (hasCallbacks() && item != null) {
            var afterIndex = item.getIndex();
            var respQuery = new ResponseItemQuery();
            respQuery.setCaller(pluginContext.getCaller());
            respQuery.setUsed(completedOutIndexes);
            respQuery.setStartAt(afterIndex);
            //respQuery.getTags().putAll(getContextTags(pluginContext.getContext()));

            var responses = storage.readResponses(getInstanceId(), respQuery);
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
                ((NetworkProtoContext) pluginContext.getContext()).addResponse(() -> sendBackResponses(pluginContext.getContext(), result));
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
    protected StorageItem readStorageItem(CompactLine index, Object in, PluginContext pluginContext) {
        return storage.readById(getInstanceId(), index.getIndex());
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
            storageItem = new StorageItem();
            storageItem.setIndex(index.getIndex());
        }
        storageItem.setConnectionId(pluginContext.getContextId());

        var lineToRead = new LineToRead(storageItem, index);
        var item = lineToRead.getStorageItem();
        completedIndexes.add((int) lineToRead.getStorageItem().getIndex());
        loadAndPrepareTheAsyncResponses(pluginContext, item);
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
    protected CompactLine findIndex(CallItemsQuery query, Object in) {
        var idx = indexes.stream()
                .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                .filter(a ->
                        typeMatching(query.getType(), a.getType()) &&
                                a.getCaller().equalsIgnoreCase(query.getCaller()) &&
                                query.getUsed().stream().noneMatch((n) -> n == a.getIndex())
                ).collect(Collectors.toList());
        CompactLine bestIndex = null;
        var maxMatch = -1;
        //Evaluate the most matching item
        for (var index : idx) {
            var currentMatch = tagsMatching(index.getTags(), query.getTags());
            if (currentMatch > maxMatch) {
                maxMatch = currentMatch;
                bestIndex = index;
            }
        }
        if (bestIndex != null) {
            log.debug("Matched for replay: {}.{}", bestIndex.getCaller(), bestIndex.getIndex());
        } else {
            log.debug("No match for reply: {}.{} {}", query.getCaller(), query.getType(), query.getTags());
        }
        return bestIndex;
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
