package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

import java.util.HashMap;

public class BasicForwardPluginSettings extends PluginSettings {
    private HashMap<String,String> mappings= new HashMap<>();

    public HashMap<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(HashMap<String, String> mappings) {
        this.mappings = mappings;
    }
}
