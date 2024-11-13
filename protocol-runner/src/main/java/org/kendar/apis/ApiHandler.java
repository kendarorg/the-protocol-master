package org.kendar.apis;

import org.kendar.apis.dtos.*;
import org.kendar.command.CommonRunner;
import org.kendar.filters.PluginDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class ApiHandler {
    private final GlobalSettings settings;

    public ApiHandler(GlobalSettings settings) {
        this.settings = settings;
    }

    private List<ProtocolInstance> instances = new ArrayList<>();

    public void addProtocol(String protocolInstanceId,
                            CommonRunner protocolManager,
                            List<PluginDescriptor> filters, ProtocolSettings settings) {
        instances.add(new ProtocolInstance(protocolInstanceId,
                protocolManager, filters, settings));
    }

    public List<ProtocolIndex> getProtocols() {
        return instances.stream().map(p -> new
                        ProtocolIndex(p.getProtocolInstanceId(), p.getProtocolManager().getId())).
                collect(Collectors.toList());
    }


    public List<FilterIndex> getProtocolFilters(String protocolInstanceId) {
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        return instance.get().getFilters().stream().map(p -> new
                        FilterIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
    }

    public Ok terminate() {
        try {
            return new Ok();
        } finally {
            for (var filter : instances) {
                filter.getProtocolManager().stop();
            }
            exit(0);
        }
    }

    public Object handleProtocolFilterActivation(String protocolInstanceId, String filterId, String action) {
        action = action.toLowerCase(Locale.ROOT);
        if (protocolInstanceId.equalsIgnoreCase("*")) {
            for (var instanceId : instances.stream().map(ProtocolInstance::getProtocolInstanceId).collect(Collectors.toList())) {
                Object x = handleProtocolFilterActivationSingle(instanceId, filterId, action);
                if (x instanceof Ko) return x;
            }

        } else {
            return handleProtocolFilterActivationSingle(protocolInstanceId, filterId, action);
        }
        return new Ok();
    }


    private Object handleProtocolFilterActivationSingle(String protocolInstanceId, String filterId, String action) {
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        if (instance.isEmpty()) {
            return new Ko("Missing protocol instance id " + protocolInstanceId);
        }
        var filterInstance = instance.get().getFilters().stream().filter(f -> f.getId().equalsIgnoreCase(filterId)).findFirst();
        switch (action) {
            case "start":
                filterInstance.get().setActive(true);
                return new Ok();
            case "stop":
                filterInstance.get().setActive(false);
                return new Ok();
            case "status":
                var status = new Status();
                status.setActive(filterInstance.get().isActive());
                return status;
        }
        return new Ko("Unknown action " + action);
    }
}