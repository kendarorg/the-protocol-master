package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.beanutils.BeanUtils;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.commons.MatchingRecRep;
import org.kendar.http.plugins.commons.SiteMatcherUtils;
import org.kendar.http.plugins.settings.HttpReplayPluginSettings;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@TpmService(tags = "http")
public class HttpReplayPlugin extends BasicReplayPlugin<HttpReplayPluginSettings> {
    private static final Logger log = LoggerFactory.getLogger(HttpReplayPlugin.class);
    private boolean blockExternal = true;
    private List<MatchingRecRep> target = new ArrayList<>();

    public HttpReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpReplayPluginSettings.class;
    }

    private Map<String, String> buildTag(Request in) {
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
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (phase == ProtocolPhase.PRE_CALL) {
                var request = (Request) in;
                var response = (Response) out;
                if (SiteMatcherUtils.matchSite((Request) in, target)) {
                    var sent = doSend(pluginContext, request, response);
                    if (!sent) {
                        if (blockExternal) {
                            response.setStatusCode(404);
                            response.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.TEXT);
                            response.setResponseText(new TextNode("Page Not Found: " + request.getMethod() + " on " + request.buildUrl()));
                            return true;
                        }
                    }
                    return sent;
                }
            }
        }
        return false;
    }

    @Override
    protected int tagsMatching(Map<String, String> tags, Map<String, String> query) {
        if (!tags.get("path").equalsIgnoreCase(query.get("path"))) {
            return -1;
        }
        if (!tags.get("host").equalsIgnoreCase(query.get("host"))) {
            return -1;
        }
        return super.tagsMatching(tags, query);
    }

    protected boolean doSend(PluginContext pluginContext, Request in, Response out) {
        var query = new CallItemsQuery();

        query.setCaller(pluginContext.getCaller());
        query.setType(in.getMethod());
        for (var tag : buildTag(in).entrySet()) {
            query.addTag(tag.getKey(), tag.getValue());
        }

        query.setUsed(completedIndexes);

        var index = findIndex(query, in);
        if (index == null) {
            if (getSettings().isBlockExternal()) {
                out.setStatusCode(500);
                out.setResponseText(new TextNode("Not Found replaying: " + in.getMethod() + " on " + in.buildUrl()));
                out.addHeader("Content-Type", ConstantsMime.TEXT);
                return true;
            }
            return false;

        }
        var storageItem = repository.readFromScenarioById(getInstanceId(), index.getLine().getIndex());
        if (storageItem == null) {
            storageItem = new StorageItem();
            storageItem.setIndex(index.getLine().getIndex());
        }

        var lineToRead = new LineToRead(storageItem, index.getLine());
        var item = lineToRead.getStorageItem();
        log.debug("Reading index {}", item.getIndex());
        var outputItem = item.retrieveOutAs(Response.class);
        if (getSettings().isRespectCallDuration()) {
            Sleeper.sleep(item.getDurationMs());
        }
        try {
            BeanUtils.copyProperties(out, outputItem);
            completedIndexes.add((int) item.getIndex());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        blockExternal = getSettings().isBlockExternal();
        target = SiteMatcherUtils.setupSites(getSettings().getTarget());
        return true;
    }


    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);

        handleSettingsChanged();
        return this;
    }

}
