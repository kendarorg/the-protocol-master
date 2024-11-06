package org.kendar.command;

import org.apache.commons.cli.*;
import org.kendar.filters.PluginDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ini.Ini;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class CommonProtocol {
    public abstract void run(String[] args, boolean isExecute, Ini go, Options options) throws Exception;

    public abstract String getDefaultPort();

    public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, Ini ini, String protocol, StorageRepository storage, ArrayList<PluginDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception;

    protected Options getCommonOptions(Options options) {
        options.addOption(createOpt("po", "port", true, "Listening port"));
        options.addOption(createOpt("pc", "connection", true, "Select remote connection string (for redis use redis://host:port"));
        options.addOption(createOpt("prp", "replay", false, "Replay from log/replay source."));
        options.addOption(createOpt("prc", "record", false, "Record to log/replay source."));
        options.addOption(createOpt("plid","replayid", true, "Set an id for the replay instance (default to timestamp_uuid)."));
        options.addOption(createOpt("pt","timeout", true, "Set timeout in seconds towards proxied system (default 30s)"));
        options.addOption(createOpt("cdt","respectcallduration", false, "Respect call duration timing"));
        return options;
    }

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

    protected void setCommonData(String[] args, Options options, Ini ini) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        var section = cmd.getOptionValue("protocol");
        ini.putValue(section, "port", Integer.parseInt(cmd.getOptionValue("port", getDefaultPort())));
        ini.putValue(section, "protocol", cmd.getOptionValue("protocol"));

        if (cmd.hasOption("replay")) {
            ini.putValue(section, "replay", cmd.hasOption("replay"));
            ini.putValue(section, "respectcallduration", cmd.hasOption("cdt"));
            ini.putValue(section, "replayid", cmd.getOptionValue("plid", UUID.randomUUID().toString()));
        }else {
            if (cmd.getOptionValue("connection") == null) {
                throw new Exception();
            }
            ini.putValue(section, "connection", cmd.getOptionValue("connection"));
        }
        if (cmd.hasOption("record")) {
            ini.putValue(section, "record", cmd.hasOption("record"));
        }
        ini.putValue(section, "timeout", Integer.parseInt(cmd.getOptionValue("timeout", "30")));
        parseExtra(ini, cmd);
    }

    protected void parseLoginPassword(Ini result, CommandLine cmd, String section) {
        result.putValue(section, "login", cmd.getOptionValue("login"));
        result.putValue(section, "password", cmd.getOptionValue("password"));
    }

    protected void optionLoginPassword(Options options) {
        options.addOption(createOpt("pu","login", true, "Select remote login"));
        options.addOption(createOpt("pw","password", true, "Select remote password"));
    }

    protected void parseExtra(Ini result, CommandLine cmd) {

    }

    public abstract String getId();
}
