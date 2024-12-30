package org.kendar.command;

import org.kendar.annotations.di.TpmService;
import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.cli.CommandParser;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings("ResultOfMethodCallIgnored")
@TpmService
public class ProtocolsRunner {
    private static final JsonMapper mapper = new JsonMapper();
    private final Map<String, CommonRunner> protocols = new HashMap<>();

    public ProtocolsRunner(List<CommonRunner> input) {
        for (CommonRunner protocol : input) {
            protocols.put(protocol.getId(), protocol);
        }
    }

    public static CommandOptions getMainOptions(ChangeableReference<GlobalSettings> settings) {


        var coptions = CommandOptions.of("main", "The Protocol Master");
        coptions.withOptions(
                CommandOption.of("un", "Unattended run (default false)")
                        .withLong("unattended")
                        .withCallback((s) -> settings.get().setUnattended(true)),
                CommandOption.of("cfg", "Load config file")
                        .withLong("config")
                        .withMandatoryParameter()
                        .withCallback((s) -> {
                            var fr = new FileResourcesUtils().getFileFromResourceAsString(s);
                            settings.set(mapper.deserialize(fr, GlobalSettings.class));
                        }),
                CommandOption.of("pld", "Plugins directory (default plugins)")
                        .withLong("pluginsDir")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.get().setPluginsDir(s)),
                CommandOption.of("dd", "Data directory (default data)")
                        .withLong("datadir")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.get().setDataDir(s)),
                CommandOption.of("ll", "Log4j loglevel (default ERROR)")
                        .withLong("loglevel")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.get().setLogLevel(s)),
                CommandOption.of("ap", "The port TPM controllers (default 0, as not active)")
                        .withLong("apis")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.get().setApiPort(Integer.parseInt(s))),
                CommandOption.of("lt", "The log type (default file)")
                        .withLong("logType")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.get().setDataDir(s)),
                CommandOption.of("p", "The protocols to start")
                        .withLong("protocol")
                        .withMandatoryParameter(),
                CommandOption.of("h", "Show help")
                        .withLong("help")
        );
        return coptions;
    }

    public static <T> T getOrDefault(Object value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    public boolean prepareSettingsFromCommandLine(CommandOptions options, String[] args,  GlobalSettings settings, CommandParser parser) {

        try {
            var protocolMotherOption = options.getCommandOption("p");
            var protocolOptionsToAdd = new ArrayList<CommandOptions>();


            var isExecute = false;
            if (parser.hasOption("help")) {
                var helpValue = parser.getOptionValue("help");
                if (helpValue == null) {
                    for (var protocol : protocols.values()) {
                        protocolOptionsToAdd.add(protocol.getOptions(settings));
                    }
                    protocolMotherOption.withSubChoices(protocolOptionsToAdd.toArray(new CommandOptions[0]));
                } else {
                    var protocol = protocols.values().stream().filter(p -> p.getId().equalsIgnoreCase(helpValue)).findFirst().get();
                    protocolOptionsToAdd.add(protocol.getOptions(settings));
                    protocolMotherOption.withSubChoices(protocolOptionsToAdd.toArray(new CommandOptions[0]));
                }
                throw new Exception();
            } else {
                isExecute = true;

                for (var protocol : protocols.values()) {
                    protocolOptionsToAdd.add(protocol.getOptions(settings));
                }
                protocolMotherOption.withSubChoices(protocolOptionsToAdd.toArray(new CommandOptions[0]));
                parser.parse(args);
                return true;
            }
        } catch (Exception ex) {
            if (ex.getMessage() != null) {
                System.err.println("ERROR: " + ex.getMessage());
            }
            parser.printHelp();
        }
        return false;
    }


    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                      GlobalSettings ini, ProtocolSettings protocol, StorageRepository storage,
                      List<ProtocolPluginDescriptor> plugins,
                      Supplier<Boolean> stopWhenFalse) throws Exception {
        var pr = protocols.get(protocol.getProtocol());
        var datadir = Path.of(ini.getDataDir()).toAbsolutePath().toFile();
        if (!datadir.exists()) {
            datadir.mkdir();
        }
        protocol.setProtocolInstanceId(key);
        pr.start(protocolServer, key, ini, protocol, storage, plugins, stopWhenFalse);
    }

    public CommonRunner getManagerFor(ProtocolSettings protocol) {
        return protocols.get(protocol.getProtocol());
    }
}
