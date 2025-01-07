package org.kendar.http.plugins;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TpmService(tags = "http")
public class HttpRateLimitPlugin extends ProtocolPluginDescriptorBase<HttpRateLimitPluginSettings> {
    private final Object sync = new Object();
    private final Logger log = LoggerFactory.getLogger(HttpRateLimitPlugin.class);
    private List<Pattern> recordSites = new ArrayList<>();
    private Calendar resetTime;
    private int resourcesRemaining = -1;
    private Response customResponse;

    public HttpRateLimitPlugin(JsonMapper mapper) {
        super(mapper);
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
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        var settings = getSettings();
        setupSitesToRecord(settings.getLimitSites());
        if (settings.getCustomResponseFile() != null && Files.exists(Path.of(settings.getCustomResponseFile()))) {
            var frr = new FileResourcesUtils();
            customResponse = mapper.deserialize(frr.getFileFromResourceAsString(settings.getCustomResponseFile()), Response.class);
        }
        return this;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (isActive()) {
            if (!recordSites.isEmpty()) {
                var matchFound = false;
                for (var pat : recordSites) {
                    if (pat.matcher(in.getHost()).matches()) {// || pat.toString().equalsIgnoreCase(request.getHost())) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    return false;
                }
            }
            return handleRateLimit(pluginContext, phase, in, out);
        }
        return false;
    }

    private void setupSitesToRecord(List<String> recordSites) {
        this.recordSites = recordSites.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(regex -> regex.startsWith("@") ?
                        Pattern.compile(regex.substring(1)) :
                        Pattern.compile(Pattern.quote(regex))).collect(Collectors.toList());
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

            var settings = (HttpRateLimitPluginSettings) getSettings();
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
                log.trace("Rate limit reached for {}", in.getHost());
                var isnt = Calendar.getInstance();
                isnt.setTimeInMillis(
                        resetTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());


                var reset = settings.getResetFormat().equalsIgnoreCase("SecondsLeft") ?
                        isnt.getTimeInMillis() / 1000 :  // drop decimals
                        resetTime.toInstant().getEpochSecond();

                //Logger.LogRequest($"Exceeded resource limit when calling {request.Url}. Request will be throttled", MessageType.Failed, new LoggingContext(e.Session));
                if (settings.getCustomResponseFile() != null && Files.exists(Path.of(settings.getCustomResponseFile()))) {
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
                log.trace("Rate limit warn for {}", in.getHost());
                out.addHeader(settings.getHeaderLimit(), settings.getRateLimit() + "");
                out.addHeader(settings.getHeaderRemaining(), resourcesRemaining + "");
            }

        }
        return false;
    }
}
