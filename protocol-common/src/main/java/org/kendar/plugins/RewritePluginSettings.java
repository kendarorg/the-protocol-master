package org.kendar.plugins;

import org.kendar.settings.PluginSettings;

public class RewritePluginSettings extends PluginSettings {
    private String rewritesFile;


    public String getRewritesFile() {
        return rewritesFile;
    }

    public void setRewritesFile(String rewritesFile) {
        this.rewritesFile = rewritesFile;
    }
}
