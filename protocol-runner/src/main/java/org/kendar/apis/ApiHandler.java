package org.kendar.apis;

import org.kendar.apis.dtos.FilterIndex;
import org.kendar.apis.dtos.ProtocolIndex;
import org.kendar.command.CommonRunner;
import org.kendar.filters.PluginDescriptor;
import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApiHandler {
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

    public void changeProtocolFilterActivation(String protocolInstanceId, String filter, boolean active) {
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        var filterInstance = instance.get().getFilters().stream().filter(f -> f.getId().equalsIgnoreCase(filter)).findFirst();
        filterInstance.get().setActive(active);
    }

//    public void changeProtocolRecordingStatus(String protocolInstanceId,String function,boolean active) {
//        var instance =instances.stream().filter(p->p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
//        var filterInstance =instance.get().getFilters().stream().filter(f->f.getId().equalsIgnoreCase(filter)).findFirst();
//        filterInstance.get().setActive(active);
//    }
}
