package org.kendar.filters;

import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Map;

public interface FilterDescriptor extends ExtensionPoint {
    List<ProtocolPhase> getPhases();

    String getId();
    String getProtocol();

    void initialize(Map<String, Object> section);

    void terminate();
    FilterDescriptor clone();
}
