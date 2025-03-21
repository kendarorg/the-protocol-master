package org.kendar.ui.dto;

import java.util.HashMap;

public class BaseHtmxDto {
    private final HashMap<String, Object> parameters = new HashMap<>();

    public HashMap<String, Object> getParameters() {
        return parameters;
    }
}
