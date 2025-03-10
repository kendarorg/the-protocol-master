package org.kendar.http.plugins;

import org.kendar.plugins.settings.BasicReplayPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpReplayPluginSettings extends BasicReplayPluginSettings {

    private List<String> matchSites = new ArrayList<>();

    public List<String> getMatchSites() {
        return matchSites;
    }

    public void setMatchSites(List<String> matchSites) {
        this.matchSites = matchSites;
    }
}
