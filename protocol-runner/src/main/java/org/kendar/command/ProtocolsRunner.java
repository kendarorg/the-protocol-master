package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.cli.StorageCli;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;

import java.util.regex.Pattern;

@TpmService
public class ProtocolsRunner {
    private static final JsonMapper mapper = new JsonMapper();
    private static final String TPM_REPLACE = "TPM_REPLACE";

    public static CommandOptions getMainOptions(ChangeableReference<GlobalSettings> settings, DiService diService) {

        var availableStorages = diService.getInstances(StorageCli.class);
        StringBuilder storageOptions = new StringBuilder();
        for (var storage : availableStorages) {
            storageOptions.append("\n- ").append(storage.getDescription());
        }
        var coptions = CommandOptions.of("main",
                "=======================\n" +
                        "= The Protocol Master =\n" +
                        "=======================\n" +
                        "If an environment variable exists " + TPM_REPLACE + "=a=b,c=d,e=f\n" +
                        "every occurrence of %a% in config file is replaced with \n" +
                        "b value and so on");
        coptions.withOptions(
                CommandOption.of("un", "Unattended run (default false)")
                        .withLong("unattended")
                        .withCallback((s) -> settings.get().setUnattended(true)),
                CommandOption.of("cfg", "Load config file")
                        .withLong("config")
                        .withMandatoryParameter()
                        .withCallback((s) -> loadConfigFile(settings, s)),
                CommandOption.of("pld", "Plugins directory (default plugins)")
                        .withLong("pluginsDir")
                        .withMandatoryParameter()
                        .withCallback((s) -> settings.get().setPluginsDir(s)),
                CommandOption.of("dd", "Data directory (default file=data)]\n*Options:" + storageOptions)
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
                CommandOption.of("h", "Show help")
                        .withLong("help"),
                CommandOption.of("p", "The protocols to start")
                        .withLong("protocol")
                        .withMandatoryParameter()
        );
        return coptions;
    }

    private static void loadConfigFile(ChangeableReference<GlobalSettings> settings, String s) {
        var tpmReplace = System.getenv(TPM_REPLACE);
        var fr = new FileResourcesUtils().getFileFromResourceAsString(s);
        if (tpmReplace != null) {
            var split = tpmReplace.split(",");
            for (var spl : split) {
                var variable = spl.split("=", 2);
                fr = fr.replaceAll(Pattern.quote("%" + variable[0] + "%"), variable[1]);
            }
        }
        settings.set(mapper.deserialize(fr, GlobalSettings.class));
    }

    public static <T> T getOrDefault(Object value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }
}
