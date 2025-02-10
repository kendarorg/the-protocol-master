package org.kendar.cli;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalSettings {
    private Map<String, ProtocolSetting> protocols = new HashMap<>();
    private String storageDir;

    public List<String> getMultiple() {
        return multiple;
    }

    private List<String> multiple;

    public Map<String, ProtocolSetting> getProtocols() {
        return protocols;
    }

    public void setProtocols(Map<String, ProtocolSetting> protocols) {
        this.protocols = protocols;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public void setMultiple(List<String> multiple) {
        this.multiple = multiple;
    }
}
