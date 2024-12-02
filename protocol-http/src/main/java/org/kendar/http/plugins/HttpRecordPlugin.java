package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.RecordPlugin;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;

import java.util.*;
import java.util.stream.Collectors;

public class HttpRecordPlugin extends RecordPlugin<HttpRecordPluginSettings> {
    private List<MatchingRecRep> recordSites = new ArrayList<>();


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
                if (pat.match(request.getHost() + request.getPath())) {// || pat.toString().equalsIgnoreCase(request.getHost())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return;
            }
        }
        var settings = (HttpRecordPluginSettings) getSettings();
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


    private void setupSitesToRecord(List<String> recordSites) {
        this.recordSites = recordSites.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(MatchingRecRep::new).collect(Collectors.toList());
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

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        withStorage((StorageRepository) global.getService("storage"));
        setupSitesToRecord(getSettings().getRecordSites());
        return this;
    }
}
