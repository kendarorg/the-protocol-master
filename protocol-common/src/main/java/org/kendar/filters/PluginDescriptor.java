package org.kendar.filters;

import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Map;

public interface PluginDescriptor extends ExtensionPoint {
    List<ProtocolPhase> getPhases();

    String getId();

    String getProtocol();

    void initialize(Map<String, Object> section, Map<String, Object> global);

    void terminate();

    PluginDescriptor clone();
}
