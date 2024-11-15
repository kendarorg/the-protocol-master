package org.kendar.command;

import org.apache.commons.cli.*;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.settings.BasicRecordingPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class CommonRunner {
    public static Option createOpt(String shortVersion, String longVersion, boolean hasArg, String description) {
        return createOpt(shortVersion, longVersion, hasArg, description, null);
    }

    public static Option createOpt(String shortVersion, String longVersion, boolean hasArg, String description, Boolean optionalArg) {
        var res = Option.builder();
        if (shortVersion != null) {
            res.option(shortVersion);
        }
        if (longVersion != null) {
            res.longOpt(longVersion);
        }
        res.hasArg(hasArg).
                desc(description);
        if (optionalArg != null) {
            res.optionalArg(optionalArg);
        }
        return res.build();
    }

    //public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, GlobalSettings ini, ProtocolSettings protocol, StorageRepository storage, Object filters, Supplier<Boolean> stopWhenFalse) throws Exception;

    public abstract void run(String[] args, boolean isExecute, GlobalSettings go,
                             Options options,
                             HashMap<String, List<PluginDescriptor>> filters) throws Exception;

    public abstract String getDefaultPort();

    protected Options getCommonOptions(Options options) {
        options.addOption(createOpt("po", "port", true, "Listening port"));
        options.addOption(createOpt("pc", "connection", true, "Select remote connection string (for redis use redis://host:port"));
        options.addOption(createOpt("prp", "replay", false, "Replay from log/replay source."));
        options.addOption(createOpt("prc", "record", false, "Record to log/replay source."));
        options.addOption(createOpt("plid", "replayid", true, "Set an id for the replay instance (default to timestamp_uuid)."));
        options.addOption(createOpt("pt", "timeout", true, "Set timeout in seconds towards proxied system (default 30s)"));
        options.addOption(createOpt("cdt", "respectcallduration", false, "Respect call duration timing"));
        return options;
    }

    protected void setCommonData(String[] args, Options options, GlobalSettings ini,
                                 ByteProtocolSettings section) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        ini.getProtocols().put(getId(), section);
        section.setPort(Integer.parseInt(cmd.getOptionValue("port", getDefaultPort())));
        section.setProtocol(cmd.getOptionValue("protocol"));
        section.setProtocolInstanceId(getId());
        section.setConnectionString(cmd.getOptionValue("connection"));
        section.setTimeoutSeconds(Integer.parseInt(cmd.getOptionValue("timeout", "30")));

        if (cmd.hasOption("replay")) {
            var plugin = new BasicReplayPluginSettings();
            plugin.setPlugin("replay-plugin");
            plugin.setActive(true);
            plugin.setRespectCallDuration(cmd.hasOption("cdt"));
            plugin.setReplayId(cmd.getOptionValue("plid", UUID.randomUUID().toString()));
            section.getPlugins().put("replay-plugin", plugin);
        } else if (cmd.hasOption("record")) {
            var plugin = new BasicRecordingPluginSettings();
            section.getPlugins().put("record-plugin", plugin);
            plugin.setPlugin("record-plugin");
            plugin.setActive(true);
        } else if (cmd.hasOption("mock")) {
            throw new RuntimeException("MISSING MOCK");
        }
        parseExtra(section, cmd);
    }

    protected void parseLoginPassword(ByteProtocolSettingsWithLogin protocol, CommandLine cmd) {
        protocol.setLogin(cmd.getOptionValue("login"));
        protocol.setPassword(cmd.getOptionValue("password"));
    }

    protected void optionLoginPassword(Options options) {
        options.addOption(createOpt("pu", "login", true, "Select remote login"));
        options.addOption(createOpt("pw", "password", true, "Select remote password"));
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {

    }

    public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                               GlobalSettings ini, ProtocolSettings protocol,
                               StorageRepository storage, List<PluginDescriptor> filters,
                               Supplier<Boolean> stopWhenFalse) throws Exception;

    public abstract String getId();

    public abstract Class<?> getSettingsClass();

    public abstract void stop();
}
