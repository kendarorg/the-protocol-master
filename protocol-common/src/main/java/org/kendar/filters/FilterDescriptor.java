package org.kendar.filters;

import java.util.Map;

public interface FilterDescriptor {
    String getId();
    String getProtocol();

    void initialize(Map<String, Object> section);

    void terminate();
}
