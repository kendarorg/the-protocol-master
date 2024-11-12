package org.kendar.http.plugins;

import org.kendar.filters.BasicRecordingPlugin;
import org.kendar.filters.PluginDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.http.utils.Request;
import org.kendar.proxy.FilterContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.StorageItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpRecordingPlugin extends BasicRecordingPlugin {
    private List<Pattern> recordSites;
    private HttpRecordPluginSettings settings;

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, Object in, Object out) {
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
                postCall(filterContext, in, out);
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
        var result = new HashMap<String,String>();
        result.put("path", in.getPath());
        result.put("host", in.getHost());
        var query = String.join("&", in.getQuery().entrySet().stream().
                sorted(Comparator.comparing(Map.Entry<String, String>::getKey)).
                map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.toList()));

        result.put("query", query);
        return result;
    }
}
