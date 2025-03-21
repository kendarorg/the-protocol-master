package org.kendar.http.plugins;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.StorageReloadedEvent;
import org.kendar.http.plugins.commons.MatchingRecRep;
import org.kendar.http.plugins.commons.SiteMatcherUtils;
import org.kendar.http.plugins.settings.HttpRateLimitPluginSettings;
import org.kendar.plugins.BasicPercentPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.PluginFileManager;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@TpmService(tags = "http")
public class HttpRateLimitPlugin extends BasicPercentPlugin<HttpRateLimitPluginSettings> {
    private final Object sync = new Object();
    private final Logger log = LoggerFactory.getLogger(HttpRateLimitPlugin.class);
    private final StorageRepository repository;
    private List<MatchingRecRep> sitesToLimit = new ArrayList<>();
    private Calendar resetTime;
    private int resourcesRemaining = -1;
    private Response customResponse;
    private PluginFileManager storage;

    public HttpRateLimitPlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper);
        this.repository = repository;
        EventsQueue.register(UUID.randomUUID().toString(), (e) -> handleSettingsChanged(), StorageReloadedEvent.class);
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpRateLimitPluginSettings.class;
    }

    @Override
    protected void handleActivation(boolean active) {
        synchronized (sync) {
            resetTime = null;
            resourcesRemaining = -1;
        }
    }

    @Override
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        sitesToLimit = SiteMatcherUtils.setupSites(getSettings().getTarget());
        customResponse = null;
        var responseFile = storage.readFile("response");
        if (responseFile != null) {
            customResponse = mapper.deserialize(responseFile, Response.class);
        }
        return true;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        storage = repository.buildPluginFileManager(getInstanceId(), getId());
        if (!handleSettingsChanged()) return null;
        return this;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (shouldRun()) {
            if (SiteMatcherUtils.matchSite(in, sitesToLimit)) {
                return handleRateLimit(pluginContext, phase, in, out);
            }
        }
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "rate-limit-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }


    private boolean handleRateLimit(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {

        synchronized (sync) {

            var settings = getSettings();
            // set the initial values for the first request
            if (resetTime == null) {
                resetTime = Calendar.getInstance();
                resetTime.setTimeInMillis(Calendar.getInstance().getTimeInMillis() + (settings.getResetTimeWindowSeconds() * 1000L));
            }
            if (resourcesRemaining == -1) {
                resourcesRemaining = settings.getRateLimit();
            }

            // see if we passed the reset time window
            if (Calendar.getInstance().after(resetTime)) {
                resourcesRemaining = settings.getRateLimit();
                resetTime = Calendar.getInstance();
                resetTime.setTimeInMillis(Calendar.getInstance().getTimeInMillis() + (settings.getResetTimeWindowSeconds() * 1000L));
            }

            // subtract the cost of the request
            resourcesRemaining -= settings.getCostPerRequest();
            if (resourcesRemaining < 0) {
                resourcesRemaining = 0;
                log.info("Rate limit reached for {}", in.buildUrl());
                var isnt = Calendar.getInstance();
                isnt.setTimeInMillis(
                        resetTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());


                var reset = settings.getResetFormat().equalsIgnoreCase("SecondsLeft") ?
                        isnt.getTimeInMillis() / 1000 :  // drop decimals
                        resetTime.toInstant().getEpochSecond();

                //Logger.LogRequest($"Exceeded resource limit when calling {request.Url}. Request will be throttled", MessageType.Failed, new LoggingContext(e.Session));
                if (customResponse != null && settings.isUseCustomResponse()) {
                    out.getHeaders().clear();
                    out.getHeaders().putAll(customResponse.getHeaders());
                    out.removeHeader(settings.getHeaderRetryAfter());
                    out.addHeader(settings.getHeaderRetryAfter(), "" + (isnt.getTimeInMillis() / 1000));
                    out.setResponseText(customResponse.getResponseText());
                    out.setStatusCode(customResponse.getStatusCode());
                    return true;
                } else {
                    out.addHeader(settings.getHeaderLimit(), settings.getRateLimit() + "");
                    out.addHeader(settings.getHeaderReset(), reset + "");
                    out.addHeader(settings.getHeaderRetryAfter(), "" + (isnt.getTimeInMillis() / 1000));
                    out.setStatusCode(429);
                    return true;
                }
            } else if (resourcesRemaining < (settings.getRateLimit() -
                    (settings.getRateLimit() * settings.getWarningThresholdPercent() / 100))) {
                log.trace("Rate limit warn for {}", in.buildUrl());
                out.addHeader(settings.getHeaderLimit(), settings.getRateLimit() + "");
                out.addHeader(settings.getHeaderRemaining(), resourcesRemaining + "");
            }

        }
        return false;
    }
}
