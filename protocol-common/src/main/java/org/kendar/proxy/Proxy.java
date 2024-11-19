package org.kendar.proxy;

import org.kendar.events.EventsQueue;
import org.kendar.events.ReplayStatusEvent;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Base proxy implementation
 *
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public abstract class Proxy {
    private final Map<String, Map<ProtocolPhase, List<ProtocolPluginDescriptor>>> allowedPlugins = new ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("(.*)\\((.*)\\)");
    protected boolean replayer;
    /**
     * Descriptor (of course network like)
     */
    private NetworkProtoDescriptor protocol;

    protected Proxy() {
        EventsQueue.register(this.toString(), this::replayChange, ReplayStatusEvent.class);
    }

    private void replayChange(ReplayStatusEvent e) {
        replayer = e.isReplaying();
    }

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

    public List<PluginDescriptor> getPlugins() {
        var result = new HashMap<String, PluginDescriptor>();
        for (var item : allowedPlugins.entrySet()) {
            for (var phase : item.getValue().entrySet()) {
                for (var plugin : phase.getValue()) {
                    result.put(plugin.getId(), plugin);
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    public void setPlugins(List<PluginDescriptor> filters) {
        for (var plugin : filters) {
            var clazz = plugin.getClass();
            var handle = Arrays.stream(clazz.getMethods()).filter(m -> m.getName().equalsIgnoreCase("handle")).findFirst();

            if (handle.isPresent()) {
                var matcher = pattern.matcher(handle.get().toString());
                if (matcher.find()) {
                    var pars = matcher.group(2);
                    if (!allowedPlugins.containsKey(pars)) {
                        allowedPlugins.put(pars, new HashMap<>());
                    }
                    var map = allowedPlugins.get(pars);
                    for (var phase : plugin.getPhases()) {
                        if (!map.containsKey(phase)) {
                            map.put(phase, new ArrayList<>());
                        }
                        map.get(phase).add((ProtocolPluginDescriptor) plugin);
                    }
                }
            }
        }
    }

    public <I, J> List<ProtocolPluginDescriptor> getPlugins(ProtocolPhase phase, I in, J out) {
        var data = String.join(",",
                PluginContext.class.getName(),
                ProtocolPhase.class.getName(),
                in.getClass().getName(), out.getClass().getName());
        var anonymousData = String.join(",",
                PluginContext.class.getName(),
                ProtocolPhase.class.getName(),
                Object.class.getName(), Object.class.getName());
        var forData = allowedPlugins.get(anonymousData);
        //Handle Object,Object data
        if (forData != null) {
            var forPhase = forData.get(phase);
            if (forPhase != null) {
                return forPhase;
            }
        }
        forData = allowedPlugins.get(data);
        if (forData != null) {
            var forPhase = forData.get(phase);
            if (forPhase != null) {
                return forPhase;
            }
        }
        return List.of();
    }

    public void terminateFilters() {
        var terminatedPlugins = new HashSet<>();
        for (var item : allowedPlugins.entrySet()) {
            for (var phase : item.getValue().entrySet()) {
                for (var plugin : phase.getValue()) {
                    if (plugin.isActive() && !terminatedPlugins.contains(plugin)) {
                        plugin.terminate();
                        terminatedPlugins.add(plugin);
                    }
                }
            }
        }
    }
}
