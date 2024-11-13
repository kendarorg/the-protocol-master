package org.kendar.http.plugins;

import org.kendar.plugins.BasicRecordingPlugin;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.http.utils.Request;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.StorageItem;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpRecordingPlugin extends BasicRecordingPlugin {
    private List<Pattern> recordSites = new ArrayList<>();
    private HttpRecordPluginSettings settings;

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (phase == ProtocolPhase.POST_CALL) {
                var request = (Request) in;
                if (recordSites.size() > 0) {
                    var matchFound = false;
                    for (var pat : recordSites) {
                        if (pat.matcher(request.getHost()).matches()) {
                            matchFound = true;
                            break;
                        }
                    }
                    if (!matchFound) {
                        return false;
                    }
                }
                postCall(pluginContext, in, out);
            }
        }
        return false;
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpRecordPluginSettings.class;
    }

    @Override
    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        settings = (HttpRecordPluginSettings) plugin;
        setupSitesToRecord(settings.getRecordSites());
    }

    private void setupSitesToRecord(List<String> recordSites) {
        this.recordSites = recordSites.stream()
                .map(s -> s.trim()).filter(s -> s.length() > 0)
                .map(s -> Pattern.compile(s)).collect(Collectors.toList());
    }


    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        super.initialize(global, protocol);
        return this;
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var in = item.retrieveInAs(Request.class);
        var result = new HashMap<String, String>();
        result.put("path", in.getPath());
        result.put("host", in.getHost());
        var query = String.join("&", in.getQuery().entrySet().stream().
                sorted(Comparator.comparing(Map.Entry<String, String>::getKey)).
                map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.toList()));

        result.put("query", query);
        return result;
    }
}
