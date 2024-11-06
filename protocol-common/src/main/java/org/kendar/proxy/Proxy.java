package org.kendar.proxy;

import org.kendar.filters.PluginDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.storage.Storage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Base proxy implementation
 *
 * @param <T>
 */
public abstract class Proxy<T extends Storage> {
    protected boolean replayer;
    /**
     * (Eventual) storage
     */
    protected T storage;
    /**
     * Descriptor (of course network like)
     */
    private NetworkProtoDescriptor protocol;
    private Map<String, Map<ProtocolPhase, List<ProtocolPluginDescriptor>>> toFilter = new ConcurrentHashMap<>();
    private Pattern pattern = Pattern.compile("(.*)\\((.*)\\)");

    public boolean isReplayer() {
        return replayer;
    }

    /**
     * Retrieve the protocol data
     *
     * @return
     */
    public NetworkProtoDescriptor getProtocol() {
        return protocol;
    }

    /**
     * Set the protocol data
     *
     * @param protocol
     */
    public void setProtocol(NetworkProtoDescriptor protocol) {
        this.protocol = protocol;
        if (this.storage != null) {
            this.storage.setDescriptor(protocol);
        }
    }

    /**
     * Implementation specific when connecting to a real server
     *
     * @param context
     * @return
     */
    public abstract ProxyConnection connect(NetworkProtoContext context);

    /**
     * Initialize the proxy
     */
    public abstract void initialize();

    /**
     * Get the storage
     *
     * @return
     */
    public T getStorage() {
        return storage;
    }

    /**
     * Set and initialize the storage
     *
     * @param storage
     */
    public void setStorage(T storage) {
        this.storage = storage;
        if (protocol != null) {
            this.storage.setDescriptor(protocol);
        }

        this.storage.initialize();
    }

    public void setFilters(List<PluginDescriptor> filters) {
        for (var filter : filters) {
            var clazz = filter.getClass();
            var handle = Arrays.stream(clazz.getMethods()).filter(m -> m.getName().equalsIgnoreCase("handle")).findFirst();

            if (handle.isPresent()) {
                var matcher = pattern.matcher(handle.get().toString());
                if (matcher.find()) {
                    var pars = matcher.group(2);
                    if (!toFilter.containsKey(pars)) {
                        toFilter.put(pars, new HashMap<>());
                    }
                    var map = toFilter.get(pars);
                    for (var phase : filter.getPhases()) {
                        if (!map.containsKey(phase)) {
                            map.put(phase, new ArrayList<>());
                        }
                        map.get(phase).add((ProtocolPluginDescriptor) filter);
                    }
                }
            }
        }
    }

    public <I, J> List<ProtocolPluginDescriptor> getFilters(ProtocolPhase phase, I in, J out) {
        var data = in.getClass().getName() + "," + out.getClass().getName();
        var forData = toFilter.get(data);
        if (forData != null) {
            var forPhase = forData.get(phase);
            if (forPhase != null) {
                return forPhase;
            }
        }
        return List.of();
    }
}
