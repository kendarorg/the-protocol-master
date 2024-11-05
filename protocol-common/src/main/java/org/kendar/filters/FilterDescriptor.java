package org.kendar.filters;

import java.util.List;
import java.util.Map;

public interface FilterDescriptor {
    List<ProtocolPhase> getPhases();

    String getId();
    String getProtocol();

    void initialize(Map<String, Object> section);

    void terminate();
}
