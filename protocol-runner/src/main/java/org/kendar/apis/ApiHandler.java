package org.kendar.apis;

import org.kendar.apis.dtos.*;
import org.kendar.command.CommonRunner;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class ApiHandler {
    private final GlobalSettings settings;
    private final ConcurrentLinkedQueue<ProtocolInstance> instances = new ConcurrentLinkedQueue<>();

    public ApiHandler(GlobalSettings settings) {
        this.settings = settings;
    }

    public void addProtocol(String protocolInstanceId,
                            CommonRunner protocolManager,
                            List<PluginDescriptor> plugins, ProtocolSettings settings) {
        instances.add(new ProtocolInstance(protocolInstanceId,
                protocolManager, plugins, settings));
    }

    public List<ProtocolIndex> getProtocols() {
        return instances.stream().map(p -> new
                        ProtocolIndex(p.getProtocolInstanceId(), p.getProtocolManager().getId())).
                collect(Collectors.toList());
    }


    public List<PluginIndex> getProtocolPlugins(String protocolInstanceId) {
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        return instance.get().getPlugins().stream().map(p -> new
                        PluginIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
    }

    public Ok terminate() {
        try {
            return new Ok();
        } finally {
            for (var plugin : instances) {
                plugin.getProtocolManager().stop();
            }
            exit(0);
        }
    }

    public Object handleProtocolPluginActivation(String protocolInstanceId, String pluginId, String action) {
        action = action.toLowerCase(Locale.ROOT);
        if (protocolInstanceId.equalsIgnoreCase("*")) {
            for (var instanceId : instances.stream().map(ProtocolInstance::getProtocolInstanceId).collect(Collectors.toList())) {
                Object x = handleProtocolPluginActivationSingle(instanceId, pluginId, action);
                if (x instanceof Ko) return x;
            }

        } else {
            return handleProtocolPluginActivationSingle(protocolInstanceId, pluginId, action);
        }
        return new Ok();
    }


    private Object handleProtocolPluginActivationSingle(String protocolInstanceId, String pluginId, String action) {
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        if (instance.isEmpty()) {
            return new Ko("Missing protocol instance id " + protocolInstanceId);
        }
        var pluginInstance = instance.get().getPlugins().stream().filter(f -> f.getId().equalsIgnoreCase(pluginId)).findFirst();
        switch (action) {
            case "start":
                pluginInstance.get().setActive(true);
                return new Ok();
            case "stop":
                pluginInstance.get().setActive(false);
                return new Ok();
            case "status":
                var status = new Status();
                status.setActive(pluginInstance.get().isActive());
                return status;
        }
        return new Ko("Unknown action " + action);
    }
}