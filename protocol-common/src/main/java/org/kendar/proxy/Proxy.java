package org.kendar.proxy;

import org.kendar.events.EventsQueue;
import org.kendar.events.ReplayStatusEvent;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public List<ProtocolPluginDescriptor> getPlugins() {
        var result = new HashMap<String, ProtocolPluginDescriptor>();
        for (var item : allowedPlugins.entrySet()) {
            for (var phase : item.getValue().entrySet()) {
                for (var plugin : phase.getValue()) {
                    result.put(plugin.getId(), plugin);
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    public void setPlugins(List<ProtocolPluginDescriptor> filters) {
        for (var plugin : filters) {
            var clazz = plugin.getClass();
            var handles = Arrays.stream(clazz.getMethods()).filter(m -> m.getName().equalsIgnoreCase("handle")).collect(Collectors.toList());

            for(var handle:handles){
                var matcher = pattern.matcher(handle.toString());
                if(handle.getParameterCount()!=4)continue;
                if(handle.getParameters()[0].getType()!=PluginContext.class ||
                        handle.getParameters()[1].getType()!=ProtocolPhase.class)continue;

                Class<?>[] pType  = handle.getParameterTypes();
                Type[] gpType = handle.getGenericParameterTypes();
                for (int i = 0; i < pType.length; i++) {
                    System.out.println("ParameterType"+ pType[i]);
                    System.out.println("GenericParameterType"+ gpType[i]);
                }

                if (matcher.find()) {
                    var pars = matcher.group(2);
                    if (!allowedPlugins.containsKey(pars)) {
                        allowedPlugins.put(pars, new HashMap<>());
                    }
                    var map = allowedPlugins.get(pars);
                    for (var phase : plugin.getPhases()) {
                        if (!map.containsKey(phase)) {
                            map.put((ProtocolPhase) phase, new ArrayList<>());
                        }
                        var result = new PluginHandler(plugin,handle.getParameterTypes()[2],handle.getParameterTypes()[3]);
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
        var result = new ArrayList<ProtocolPluginDescriptor>();
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
                    if (plugin.isActive() && !terminatedPlugins.contains(plugin)) {
                        plugin.terminate();
                        terminatedPlugins.add(plugin);
                    }
                }
            }
        }
    }
}
