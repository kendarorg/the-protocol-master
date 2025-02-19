package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.HttpProtocol;
import org.kendar.http.plugins.*;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@TpmService
public class HttpRunner extends CommonRunner {
    private static final Logger log = LoggerFactory.getLogger(HttpRunner.class);
    private TcpServer ps;


    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new HttpProtocolSettings();
        settings.setProtocol(getId());
        var recording = new HttpRecordPluginSettings();
        var replaying = new HttpReplayPluginSettings();
        var rewrite = new RewritePluginSettings();
        var error = new HttpErrorPluginSettings();
        var latency = new HttpLatencyPluginSettings();
        List<CommandOption> commandOptionList = new ArrayList<>(List.of(
                CommandOption.of("ht", "Http port (default " + settings.getHttp())
                        .withLong("http")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.setHttp(Integer.parseInt(s))),
                CommandOption.of("hs", "Https port (default " + settings.getHttps())
                        .withLong("https")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.setHttps(Integer.parseInt(s))),
                CommandOption.of("prx", "Proxy port (default " + settings.getProxy())
                        .withLong("proxy")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.setProxy(Integer.parseInt(s))),
                CommandOption.of("cn", "Cname (default " + settings.getSSL().getCname())
                        .withLong("cname")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.getSSL().setDer(s)),
                CommandOption.of("der", "Der file (default " + settings.getSSL().getDer())
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.getSSL().setDer(s)),
                CommandOption.of("key", "Key file (default " + settings.getSSL().getKey())
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.getSSL().setKey(s)),
                CommandOption.of("rew", "Path of the rewrite queries file")
                        .withLong("rewrite")
                        .withCallback((s) -> {
                            settings.getPlugins().put("rewrite-plugin", rewrite);
                            rewrite.setActive(true);
                        }),
                CommandOption.of("record", "Record Calls")
                        .withCallback(s -> {
                            recording.setActive(true);
                            settings.getPlugins().put("record-plugin", recording);
                        }),
                CommandOption.of("replay", "Replay calls")
                        .withCommandOptions(
                                CommandOption.of("cdt", "Respect call duration timing\n" +
                                                "and distance between calls if applicable")
                                        .withLong("respectcallduration")
                                        .withCallback((s) -> replaying.setRespectCallDuration(true)),
                                CommandOption.of("plid", "Set an id for the replay instance\n" +
                                                "(default to timestamp_uuid).")
                                        .withMandatoryParameter()
                                        .withLong("replayid")
                                        .withCallback(replaying::setReplayId),
                                CommandOption.of("ae", "Allow external calls\n" +
                                                "(default to false if not set).")
                                        .withMandatoryParameter()
                                        .withLong("allowExternal")
                                        .withCallback((s) -> replaying.setBlockExternal(false))
                        )
                        .withCallback(s -> {
                            replaying.setActive(true);
                            settings.getPlugins().put("replay-plugin", replaying);
                        }),
                CommandOption.of("errors", "Random errors")
                        .withCommandOptions(
                                CommandOption.of("showError", "Http error code to show\n" +
                                                "(default 0 aka none)")
                                        .withCallback((s) -> error.setShowError(Integer.parseInt(s))),
                                CommandOption.of("errorPercent", "Percentage of calls in error\n" +
                                                "(default 0 aka none)")
                                        .withCallback((s) -> error.setErrorPercent(Integer.parseInt(s))),
                                CommandOption.of("errorMessage", "Error message to show\n" +
                                                "(default `Error`)")
                                        .withCallback(error::setErrorMessage)
                        )
                        .withCallback(s -> {
                            recording.setActive(true);
                            settings.getPlugins().put("error-plugin", error);
                        }),
                CommandOption.of("latency", "Random latency")
                        .withCommandOptions(
                                CommandOption.of("latencyMin", "Minimum latency milliseconds\n" +
                                                "(default 0)")
                                        .withCallback((s) -> latency.setMinMs(Integer.parseInt(s))),

                                CommandOption.of("latencyMax", "Maximum latency milliseconds\n" +
                                                "(default 0)")
                                        .withCallback((s) -> latency.setMaxMs(Integer.parseInt(s)))
                        )
                        .withCallback(s -> {
                            recording.setActive(true);
                            settings.getPlugins().put("latency-plugin", latency);
                        })));


        return CommandOptions.of(getId())
                .withDescription(getId() + " Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }

    @Override
    public String getDefaultPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected String getConnectionDescription() {
        return "";
    }

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer,
                      String sectionKey, GlobalSettings ini, ProtocolSettings pset, StorageRepository storage,
                      List<ProtocolPluginDescriptor> plugins,
                      Supplier<Boolean> stopWhenFalseAction) throws Exception {
        var settings = (HttpProtocolSettings) pset;
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            var specificPluginSetting = pset.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (specificPluginSetting != null || plugin instanceof SSLDummyPlugin) {
                plugin.initialize(ini, settings, specificPluginSetting);
                plugin.refreshStatus();
            } else {
                plugins.remove(i);
            }
        }
        var baseProtocol = new HttpProtocol(ini, settings, plugins);
        baseProtocol.setSettings(settings);
        baseProtocol.initialize();
        var diService = DiService.getThreadContext();
        ps = new TcpServer(baseProtocol);
        ps.setOnStart(() -> DiService.setThreadContext(diService));
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(sectionKey, ps);
    }


    @Override
    public String getId() {
        return "http";
    }

    @Override
    public Class<?> getSettingsClass() {
        return HttpProtocolSettings.class;
    }

    @Override
    public void stop() {
        ps.stop();
    }
}
