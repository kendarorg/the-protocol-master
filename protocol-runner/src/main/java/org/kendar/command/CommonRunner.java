package org.kendar.command;

import org.apache.commons.cli.*;
import org.kendar.filters.PluginDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.*;
import org.kendar.storage.generic.StorageRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class CommonRunner {
    public abstract void run(String[] args, boolean isExecute, GlobalSettings go,
                             Options options,
                             HashMap<String, List<PluginDescriptor>> filters) throws Exception;

    public abstract String getDefaultPort();

    //public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, GlobalSettings ini, ProtocolSettings protocol, StorageRepository storage, Object filters, Supplier<Boolean> stopWhenFalse) throws Exception;

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

    protected void setCommonData(String[] args, Options options, GlobalSettings ini,
                                 ByteProtocolSettings section) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        ini.getProtocols().put(getId(),section);
        section.setPort(Integer.parseInt(cmd.getOptionValue("port", getDefaultPort())));
        section.setProtocol(cmd.getOptionValue("protocol"));
        section.setConnectionString(cmd.getOptionValue("connection"));
        section.setTimeoutSeconds(Integer.parseInt(cmd.getOptionValue("timeout", "30")));
        DefaultSimulationSettings simulation =null;
        if (cmd.hasOption("replay")) {
            simulation= new DefaultSimulationSettings();
            simulation.setReplay(cmd.hasOption("replay"));
            simulation.setRespectCallDuration(cmd.hasOption("cdt"));
            simulation.setReplayId(cmd.getOptionValue("plid", UUID.randomUUID().toString()));
        }else if (cmd.hasOption("record")) {
            simulation= new DefaultSimulationSettings();
            simulation.setRecord(cmd.hasOption("record"));
        }else if (cmd.hasOption("mock")) {
            simulation= new DefaultSimulationSettings();
            simulation.setMock(cmd.hasOption("mock"));
        }
        section.setSimulation(simulation);
        parseExtra(section, cmd);
    }

    protected void parseLoginPassword(ByteProtocolSettingsWithLogin protocol, CommandLine cmd) {
        protocol.setLogin(cmd.getOptionValue("login"));
        protocol.setPassword(cmd.getOptionValue("password"));
    }

    protected void optionLoginPassword(Options options) {
        options.addOption(createOpt("pu","login", true, "Select remote login"));
        options.addOption(createOpt("pw","password", true, "Select remote password"));
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {

    }

    public abstract void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                               GlobalSettings ini, ProtocolSettings protocol,
                               StorageRepository storage, List<PluginDescriptor> filters,
                               Supplier<Boolean> stopWhenFalse) throws Exception;

    public abstract String getId();

    public abstract Class<?> getSettingsClass();
}
