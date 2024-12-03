package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.beanutils.BeanUtils;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.http.utils.constants.ConstantsHeader;
import org.kendar.http.utils.constants.ConstantsMime;
import org.kendar.plugins.ReplayPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class HttpReplayPlugin extends ReplayPlugin<HttpReplayPluginSettings> {
    private static final Logger log = LoggerFactory.getLogger(HttpReplayPlugin.class);
    private boolean blockExternal = true;
    private List<MatchingRecRep> matchSites = new ArrayList<>();

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
                if (!matchSites.isEmpty()) {
                    var matchFound = false;
                    for (var pat : matchSites) {
                        if (pat.match(request.getHost() + request.getPath())) {// || pat.toString().equalsIgnoreCase(request.getHost())) {
                            matchFound = true;
                            break;
                        }
                    }
                    if (!matchFound) {
                        return false;
                    }
                }
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
        return false;
    }


    protected boolean doSend(PluginContext pluginContext, Request in, Response out) {
        var query = new CallItemsQuery();
        var context = pluginContext.getContext();

        query.setCaller(pluginContext.getCaller());
        query.setType(in.getMethod());
        for (var tag : buildTag(in).entrySet()) {
            query.addTag(tag.getKey(), tag.getValue());
        }

        query.setUsed(completedIndexes);

        var index = findIndex(query);
        if (index == null) {
            return false;
        }
        var storageItem = storage.readById(getInstanceId(), index.getIndex());
        if (storageItem == null) {
            storageItem = new StorageItem();
            storageItem.setIndex(index.getIndex());
        }

        var lineToRead = new LineToRead(storageItem, index);
        var item = lineToRead.getStorageItem();
        log.debug("READING {}", item.getIndex());
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

    private void setupMatchSites(List<String> recordSites) {
        this.matchSites = recordSites.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(MatchingRecRep::new).collect(Collectors.toList());
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);

        blockExternal = getSettings().isBlockExternal();
        setupMatchSites(getSettings().getMatchSites());
        return this;
    }

}
