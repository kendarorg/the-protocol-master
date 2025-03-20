package org.kendar.http.plugins;

import org.kendar.apis.base.Request;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.commons.MatchingRecRep;
import org.kendar.http.plugins.commons.SiteMatcherUtils;
import org.kendar.http.plugins.settings.HttpRecordPluginSettings;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;

import java.util.*;
import java.util.stream.Collectors;

@TpmService(tags = "http")
public class HttpRecordPlugin extends BasicRecordPlugin<HttpRecordPluginSettings> {
    private List<MatchingRecRep> target = new ArrayList<>();

    public HttpRecordPlugin(JsonMapper mapper, StorageRepository storage,
                            MultiTemplateEngine resolversFactory, SimpleParser parser) {
        super(mapper, storage, resolversFactory, parser);
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL);
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpRecordPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var request = (Request) in;
        if (SiteMatcherUtils.matchSite((Request) in, target)) {
            var settings = getSettings();
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
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        super.handleSettingsChanged();
        target = SiteMatcherUtils.setupSites(getSettings().getTarget());
        return true;
    }
}
