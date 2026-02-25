package org.kendar.settings;

import java.util.HashMap;

public class JdbcRewritePluginSettings extends PluginSettings {
    private HashMap<String,String> mappings= new HashMap<>();

    public HashMap<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(HashMap<String, String> mappings) {
        this.mappings = mappings;
    }
}
