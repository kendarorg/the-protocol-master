package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.RecordingPlugin;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.StorageItem;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpRecordingPlugin extends RecordingPlugin {
    private List<Pattern> recordSites = new ArrayList<>();
    private HttpRecordPluginSettings settings;


    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL);
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var request = (Request) in;
        if (!recordSites.isEmpty()) {
            var matchFound = false;
            for (var pat : recordSites) {
                if (pat.matcher(request.getHost()).matches()) {// || pat.toString().equalsIgnoreCase(request.getHost())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return;
            }
        }

        if (settings.isRemoveEtags()) {
            var all = request.getHeader("If-none-match");
            if (all != null && !all.isEmpty()) all.clear();
            all = request.getHeader("If-match");
            if (all != null && !all.isEmpty()) all.clear();
            all = request.getHeader("If-modified-since");
            if (all != null && !all.isEmpty()) all.clear();
            all = request.getHeader("ETag");
            if (all != null && !all.isEmpty()) all.clear();
        }
        super.postCall(pluginContext, in, out);
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpRecordPluginSettings.class;
    }

    @Override
    public PluginDescriptor setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        settings = (HttpRecordPluginSettings) plugin;
        setupSitesToRecord(settings.getRecordSites());
        return this;
    }

    private void setupSitesToRecord(List<String> recordSites) {
        this.recordSites = recordSites.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(regex -> regex.startsWith("@") ?
                        Pattern.compile(regex.substring(1)) :
                        Pattern.compile(Pattern.quote(regex))).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var in = item.retrieveInAs(Request.class);
        var result = new HashMap<String, String>();
        result.put("path", in.getPath());
        result.put("host", in.getHost());
        var query = in.getQuery().entrySet().stream().
                sorted(Comparator.comparing(Map.Entry<String, String>::getKey)).
                map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));

        result.put("query", query);
        return result;
    }

}
