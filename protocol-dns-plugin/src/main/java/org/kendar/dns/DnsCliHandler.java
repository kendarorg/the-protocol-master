package org.kendar.dns;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.ArrayList;
import java.util.List;

@Extension
@TpmService(tags = "dns")
public class DnsCliHandler extends NetworkProtocolCommandLineHandler implements ExtensionPoint {

    @Override
    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        var options = new ArrayList<CommandOption>();
        var settings = (DnsProtocolSettings) genericSettings;
        settings.setChildDns(List.of("8.8.8.8"));
        options.addAll(
                List.of(
                        CommandOption.of("dp", "Dns port (default 53)")
                                .withLong("port")
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.setPort(Integer.parseInt(s))),
                        CommandOption.of("cc", "Cache DNS requests (default false)")
                                .withLong("cacheDns")
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.setUseCache(Boolean.parseBoolean(s))),
                        CommandOption.of("cd", "Child dns (default 8.8.8.8)")
                                .withLong("childDns")
                                .withMandatoryParameter()
                                .asMultiple()
                                .withMultiCallback(settings::setChildDns)

                )

        );


        return options;
    }

    @Override
    protected String getConnectionDescription() {
        return "";
    }


    @Override
    protected String getDefaultPort() {
        return "53";
    }

    @Override
    public String getId() {
        return "dns";
    }

    @Override
    public String getDescription() {
        return "Dns Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new DnsProtocolSettings();
    }
}
