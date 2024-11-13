package org.kendar.http.plugins;

import org.kendar.plugins.settings.BasicReplayPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpReplayPluginSettings extends BasicReplayPluginSettings {

    private boolean blockExternal;
    private List<String> matchSites = new ArrayList<>();

    public List<String> getMatchSites() {
        return matchSites;
    }

    public void setMatchSites(List<String> matchSites) {
        this.matchSites = matchSites;
    }


    public boolean isBlockExternal() {
        return blockExternal;
    }

    public void setBlockExternal(boolean blockExternal) {
        this.blockExternal = blockExternal;
    }
}
