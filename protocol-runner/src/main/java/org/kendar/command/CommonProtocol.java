package org.kendar.command;

import org.apache.commons.cli.*;
import org.kendar.filters.FilterDescriptor;
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

    public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, Ini ini, String protocol, StorageRepository storage, ArrayList<FilterDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception;

    protected Options getCommonOptions(Options options) {
        options.addOption(Option.builder().option("port").optionalArg(true).
                desc("TPM Listening port default " + getDefaultPort()).build());
        options.addOption("connection", true, "Select remote connection string (for redis use redis://host:port");
        options.addOption("replay", false, "Replay from log/replay source.");
        options.addOption("plid", true, "Set an id for the replay instance (default to timestamp_uuid).");
        options.addOption("timeout", true, "Set timeout in seconds towards proxied system (default 30s)");
        options.addOption("cdt", false, "Respect call duration timing");
        return options;
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
        } else {
            if (cmd.getOptionValue("connection") == null) {
                throw new Exception();
            }
            ini.putValue(section, "connection", cmd.getOptionValue("connection"));
        }
        ini.putValue(section, "timeout", Integer.parseInt(cmd.getOptionValue("timeout", "30")));
        parseExtra(ini, cmd);
    }

    protected void parseLoginPassword(Ini result, CommandLine cmd, String section) {
        result.putValue(section, "login", cmd.getOptionValue("login"));
        result.putValue(section, "password", cmd.getOptionValue("password"));
    }

    protected void optionLoginPassword(Options options) {
        options.addOption("login", true, "Select remote login");
        options.addOption("password", true, "Select remote password");
    }

    protected void parseExtra(Ini result, CommandLine cmd) {

    }

    public abstract String getId();
}
