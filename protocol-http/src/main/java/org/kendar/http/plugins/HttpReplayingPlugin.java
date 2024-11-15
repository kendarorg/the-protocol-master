package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.beanutils.BeanUtils;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReplayStatusEvent;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.http.utils.constants.ConstantsHeader;
import org.kendar.http.utils.constants.ConstantsMime;
import org.kendar.plugins.ReplayingPlugin;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.utils.Sleeper;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpReplayingPlugin extends ReplayingPlugin {
    private HttpReplayPluginSettings settings;
    private boolean blockExternal = true;
    private List<Pattern> matchSites = new ArrayList<>();

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> result) {

    }

    @Override
    protected void handleActivation(boolean active) {
        EventsQueue.send(new ReplayStatusEvent(active,getProtocol(),getId(),getInstanceId()));
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
                if (!matchSites.isEmpty()) {
                    var matchFound = false;
                    for (var pat : matchSites) {
                        if (pat.matcher(request.getHost()).matches()) {
                            matchFound = true;
                            break;
                        }
                    }
                    if (!matchFound) {
                        return false;
                    }
                }
                if (blockExternal && !doSend(pluginContext, request, response)) {
                    response.setStatusCode(404);
                    response.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.TEXT);
                    response.setResponseText(new TextNode("Page Not Found: " + request.getMethod() + " on " + request.buildUrl()));
                    return true;
                }
                return false;
            }
        }
        return false;
    }


    protected boolean doSend(PluginContext pluginContext, Request in, Response out) {
        var query = new CallItemsQuery();
        var context = pluginContext.getContext();

        query.setCaller(pluginContext.getCaller());
        query.setType(in.getClass().getSimpleName());
        for (var tag : buildTag(in).entrySet()) {
            query.addTag(tag.getKey(), tag.getValue());
        }

        query.setUsed(completedIndexes);
        var lineToRead = storage.read(getInstanceId(), query);
        if (lineToRead == null) {
            return false;
        }

        var item = lineToRead.getStorageItem();
        var outputItem = item.retrieveOutAs(Response.class);
        if (context.isUseCallDurationTimes()) {
            Sleeper.sleep(item.getDurationMs());
        }
        try {
            BeanUtils.copyProperties(out, outputItem);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    private void setupSitesToRecord(List<String> recordSites) {
        this.matchSites = recordSites.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpReplayPluginSettings.class;
    }

    @Override
    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        settings = (HttpReplayPluginSettings) plugin;
        blockExternal = settings.isBlockExternal();
        setupSitesToRecord(settings.getMatchSites());
    }

}
