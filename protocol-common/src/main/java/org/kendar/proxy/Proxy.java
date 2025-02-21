package org.kendar.proxy;

import org.kendar.events.EventsQueue;
import org.kendar.events.ReplayStatusEvent;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
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
    private final Map<String, Map<ProtocolPhase, List<PluginHandler>>> allowedPlugins = new ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("(.*)\\((.*)\\)");
    protected boolean replayer;
    /**
     * Descriptor (of course network like)
     */
    private NetworkProtoDescriptor protocol;

    protected Proxy() {
        EventsQueue.register(this.toString(), this::replayChange, ReplayStatusEvent.class);
    }

    public List<PluginHandler> getAllPlugins() {
        var result = new HashMap<Class<?>, PluginHandler>();
        for (var entry : allowedPlugins.values()) {
            for (var pliList : entry.values()) {
                for (var pli : pliList) {
                    result.put(pli.getClass(), pli);
                }
            }
        }
        return new ArrayList<>(result.values());
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
        for (var item : allowedPlugins.values()) {
            for (var subitem : item.values()) {
                for (var subSubItem : subitem) {
                    subSubItem.setProtocol(protocol);
                }
            }
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

    public List<PluginHandler> getPluginHandlers() {
        var result = new HashMap<String, PluginHandler>();
        for (var item : allowedPlugins.entrySet()) {
            for (var phase : item.getValue().entrySet()) {
                for (var plugin : phase.getValue()) {
                    result.put(plugin.getId(), plugin);
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    public void setPluginHandlers(List<BasePluginDescriptor> filters) {
        for (var plugin : filters) {
            var handlers = PluginHandler.of((ProtocolPluginDescriptor) plugin, this.protocol);
            for (var handler : handlers) {
                var pars = handler.getKey();
                if (!allowedPlugins.containsKey(pars)) {
                    allowedPlugins.put(pars, new HashMap<>());
                }
                var map = allowedPlugins.get(pars);
                for (var phase : ((ProtocolPluginDescriptor) plugin).getPhases()) {
                    if (!map.containsKey(phase)) {
                        map.put((ProtocolPhase) phase, new ArrayList<>());
                    }
                    map.get(phase).add(handler);
                }
            }
        }
    }

    public <I, J> List<PluginHandler> getPluginHandlers(ProtocolPhase phase, I in, J out) {
        var data = String.join(",",
                in.getClass().getName(), out.getClass().getName());
        var anonymousData = String.join(",",
                Object.class.getName(), Object.class.getName());
        var forData = allowedPlugins.get(anonymousData);
        var result = new ArrayList<PluginHandler>();
        //Handle Object,Object data
        if (forData != null) {
            var forPhase = forData.get(phase);
            if (forPhase != null) {
                result.addAll(forPhase);
            }
        }
        forData = allowedPlugins.get(data);
        if (forData != null) {
            var forPhase = forData.get(phase);
            if (forPhase != null) {
                result.addAll(forPhase);
            }
        }
        return result;
    }

    public void terminateFilters() {
        var terminatedPlugins = new HashSet<>();
        for (var item : allowedPlugins.entrySet()) {
            for (var phase : item.getValue().entrySet()) {
                for (var plugin : phase.getValue()) {
                    if (plugin.getTarget().isActive() && !terminatedPlugins.contains(plugin)) {
                        plugin.getTarget().terminate();
                        terminatedPlugins.add(plugin);
                    }
                }
            }
        }
    }
}
