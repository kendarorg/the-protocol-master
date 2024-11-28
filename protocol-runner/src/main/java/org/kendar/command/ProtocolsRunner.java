package org.kendar.command;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.cli.CommandParser;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ProtocolsRunner {
    private final Map<String, CommonRunner> protocols = new HashMap<>();

    public ProtocolsRunner(CommonRunner... input) {
        for (CommonRunner protocol : input) {
            protocols.put(protocol.getId(), protocol);
        }
    }
    private static JsonMapper mapper = new JsonMapper();
    public static CommandOptions getMainOptions(GlobalSettings settings) {


        var contained = new ChangeableReference<>(settings);
        var coptions = CommandOptions.of("main","The Protocol Master");
        coptions.withOptions(
                CommandOption.of("un", "Unattended run (default false)")
                        .withLong("unattended")
                        .withCallback((s)->settings.setUnattended(true)),
                CommandOption.of("cfg", "Load config file")
                        .withLong("config")
                        .withMandatoryParameter()
                        .withCallback((s)->{
                            var fr = new FileResourcesUtils().getFileFromResourceAsString(s);
                            contained.set(mapper.deserialize(fr,GlobalSettings.class));
                        }),
                CommandOption.of("pld", "Plugins directory (default plugins)")
                        .withLong("pluginsDir")
                        .withMandatoryParameter()
                        .withCallback((s)->settings.setPluginsDir(s)),
                CommandOption.of("ll", "Log4j loglevel (default ERROR)")
                        .withLong("loglevel")
                        .withMandatoryParameter()
                        .withCallback((s)->settings.setLogLevel(s)),
                CommandOption.of("ap", "The port TPM controllers (default 0, as not active)")
                        .withLong("apis")
                        .withMandatoryParameter()
                        .withCallback((s)->settings.setApiPort(Integer.parseInt(s))),
                CommandOption.of("lt", "The log type (default file)")
                        .withLong("logType")
                        .withMandatoryParameter()
                        .withCallback((s)->settings.setDataDir(s)),
                CommandOption.of("h", "Show help")
                        .withLong("help")
                        .withCallback((s)->{throw new RuntimeException();})
        );
        return coptions;
    }

    public static <T> T getOrDefault(Object value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    @SuppressWarnings("ConstantValue")
    public GlobalSettings run(CommandOptions options, String[] args, HashMap<String, List<PluginDescriptor>> filters, GlobalSettings settings, CommandParser parser) {

        try {

            var isExecute = false;
            if (parser.hasOption("help")) {
                var helpValue = parser.getOptionValue("help");
                runWithParams(args, helpValue, isExecute, null, options, filters);
                throw new Exception();
            } else {
                isExecute = true;



                /*var datadir = cmd.getOptionValue("datadir", "data");
                var pluginsDir = cmd.getOptionValue("pluginsDir", "plugins");
                var protocol = cmd.getOptionValue("protocol");
                var loglevel = cmd.getOptionValue("loglevel", "ERROR");
                var logType = cmd.getOptionValue("logType", "file");
                var tpmApi = Integer.parseInt(cmd.getOptionValue("apis", "0"));
                checkOptions(settings.getDataDir(), settings.getPluginsDir(), protocol);*/
                /*var ini = new GlobalSettings();
                ini.setDataDir(datadir);
                ini.setPluginsDir(pluginsDir);
                ini.setLogLevel(loglevel);
                ini.setLogType(logType);
                ini.setApiPort(tpmApi);*/
                runWithParams(args, protocol, isExecute, ini, options, filters);
                return ini;
            }
        } catch (Exception ex) {
            if (ex.getMessage() != null) {
                System.err.println("ERROR: " + ex.getMessage());
            }
            parser.printHelp();
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
