package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.tcpserver.TcpServer;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class CommonRunner {

    public abstract String getDefaultPort();

    protected List<CommandOption> getCommonOptions(GlobalSettings globalSettings, ByteProtocolSettings settings, BasicRecordPluginSettings recording, BasicReplayPluginSettings replaying, List<CommandOption> extraOptions) {
        settings.setPort(Integer.parseInt(getDefaultPort()));
        var options = new ArrayList<>(List.of(
                CommandOption.of("po", "Listening port (default " + getDefaultPort() + ")")
                        .withLong("port")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.setPort(Integer.parseInt(s))),
                CommandOption.of("pc", "Connection (example " + getConnectionDescription() + ")")
                        .withLong("connection")
                        .withMandatoryParameter()
                        .withCallback(settings::setConnectionString),
                CommandOption.of("pt", "Timeout (deafult " + settings.getTimeoutSeconds() + ")")
                        .withLong("timeout")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.setTimeoutSeconds(Integer.parseInt(s)))));
        options.addAll(extraOptions);

        options.add(CommandOption.of("record", "Record Calls")
                .withCallback(s -> {
                    recording.setActive(true);
                    settings.getPlugins().put("record-plugin", recording);
                }));
        options.add(CommandOption.of("replay", "Replay calls")
                .withCommandOptions(
                        CommandOption.of("cdt", "Respect call duration timing\n" +
                                        "and distance between calls if applicable")
                                .withLong("respectcallduration")
                                .withCallback((s) -> replaying.setRespectCallDuration(true)),
                        CommandOption.of("plid", "Set an id for the replay instance\n" +
                                        "(default to timestamp_uuid).")
                                .withMandatoryParameter()
                                .withLong("replayid")
                                .withCallback(replaying::setReplayId)
                )
                .withCallback(s -> {
                    replaying.setActive(true);
                    settings.getPlugins().put("replay-plugin", replaying);
                }));
        return new ArrayList<>(options);
    }

    protected abstract String getConnectionDescription();


    protected List<CommandOption> optionLoginPassword(ByteProtocolSettingsWithLogin settings) {
        var options = List.of(
                CommandOption.of("pu", "Remote login")
                        .withLong("login")
                        .withMandatoryParameter()
                        .withCallback(settings::setLogin),

                CommandOption.of("pw", "Remote password")
                        .withLong("password")
                        .withMandatoryParameter()
                        .withCallback(settings::setPassword));

        return new ArrayList<>(options);
    }

    public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                               GlobalSettings ini, ProtocolSettings protocol,
                               StorageRepository storage, List<ProtocolPluginDescriptor> filters,
                               Supplier<Boolean> stopWhenFalse) throws Exception;

    public abstract String getId();

    public abstract Class<?> getSettingsClass();

    public abstract void stop();

    public abstract CommandOptions getOptions(GlobalSettings settings);
}
