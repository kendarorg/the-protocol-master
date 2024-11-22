package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.settings.SettingsManager;
import org.kendar.storage.generic.StorageRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.kendar.command.CommonRunner.createOpt;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ProtocolsRunner {
    private final Map<String, CommonRunner> protocols = new HashMap<>();

    public ProtocolsRunner(CommonRunner... input) {
        for (CommonRunner protocol : input) {
            protocols.put(protocol.getId(), protocol);
        }
    }

    public static Options getMainOptions() {
        Options options = new Options();
        options.addOption("un", "unattended", false, "Unattended run");
        options.addOption(createOpt("cfg", null, true, "Load config file"));
        options.addOption(createOpt("pld", "pluginsDir", true, "Plugins directory"));
        options.addOption(createOpt("dd", "datadir", true, "Data directory/connection string"));
        options.addOption(createOpt("ll", "loglevel", true, "Log4j log level"));
        options.addOption(createOpt("ap", "apis", true, "The port TPM controllers (def 0, as not active)"));
        options.addOption(createOpt("lt", "logType", true, "The log type: default [none|file]"));
        options.addOption(createOpt("p", "protocol", true, "Protocol (http|mqtt|amqp091|mysql|postgres|redis|mongo"));
        options.addOption(Option.builder().option("help").optionalArg(true).desc("Show contestual help").build());
        options.addOption(Option.builder().option("tpmapi").optionalArg(true).desc("Expose The Protocol Master apis on port").build());
        return options;
    }

    public static <T> T getOrDefault(Object value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    @SuppressWarnings("ConstantValue")
    public GlobalSettings run(CommandLine cmd, String[] args, HashMap<String, List<PluginDescriptor>> filters) {
        var options = getMainOptions();
        try {

            var isExecute = false;
            if (cmd.hasOption("cfg")) {
                var configFile = cmd.getOptionValue("cfg");
                return SettingsManager.load(configFile);
            } else if (cmd.hasOption("help")) {
                var helpValue = cmd.getOptionValue("help");
                checkOptions(helpValue);
                runWithParams(args, helpValue, isExecute, null, options, filters);
                throw new Exception();
            } else {
                isExecute = true;

                var datadir = cmd.getOptionValue("datadir", "data");
                var pluginsDir = cmd.getOptionValue("pluginsDir", "plugins");
                var protocol = cmd.getOptionValue("protocol");
                var loglevel = cmd.getOptionValue("loglevel", "ERROR");
                var logType = cmd.getOptionValue("logType", "file");
                var tpmApi = Integer.parseInt(cmd.getOptionValue("apis", "0"));
                checkOptions(datadir, pluginsDir, protocol);
                var ini = new GlobalSettings();
                ini.setDataDir(datadir);
                ini.setPluginsDir(pluginsDir);
                ini.setLogLevel(loglevel);
                ini.setLogType(logType);
                ini.setApiPort(tpmApi);
                runWithParams(args, protocol, isExecute, ini, options, filters);
                return ini;
            }
        } catch (Exception ex) {
            if (ex.getMessage() != null) {
                System.err.println("ERROR: " + ex.getMessage());
            }
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }
        return null;
    }

    private void checkOptions(String... args) throws Exception {
        for (var arg : args) {
            if (arg == null) {
                throw new Exception();
            }
        }
    }

    private void runWithParams(String[] args, String protocol, boolean isExecute,
                               GlobalSettings go, Options options,
                               HashMap<String, List<PluginDescriptor>> filters) throws Exception {
        var founded = protocols.get(protocol);
        founded.run(args, isExecute, go, options, filters);
    }

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                      GlobalSettings ini, ProtocolSettings protocol, StorageRepository storage, List<PluginDescriptor> filters,
                      Supplier<Boolean> stopWhenFalse) throws Exception {
        var pr = protocols.get(protocol.getProtocol());
        var datadir = Path.of(ini.getDataDir()).toAbsolutePath().toFile();
        if (!datadir.exists()) {
            datadir.mkdir();
        }
        protocol.setProtocolInstanceId(key);
        pr.start(protocolServer, key, ini, protocol, storage, filters, stopWhenFalse);
    }

    public CommonRunner getManagerFor(ProtocolSettings protocol) {
        return protocols.get(protocol.getProtocol());
    }
}
