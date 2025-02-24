package org.kendar.http.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.HttpProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;

@TpmService(tags = "http")
public class HttpCommandLineHandler extends NetworkProtocolCommandLineHandler {

    @Override
    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        var options = new ArrayList<CommandOption>();
        var settings = (HttpProtocolSettings) genericSettings;
        options.addAll(
                List.of(
                        CommandOption.of("ht", "Http port (default " + settings.getHttp())
                                .withLong("http")
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.setHttp(Integer.parseInt(s))),
                        CommandOption.of("hs", "Https port (default " + settings.getHttps())
                                .withLong("https")
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.setHttps(Integer.parseInt(s))),
                        CommandOption.of("prx", "Proxy port (default " + settings.getProxy())
                                .withLong("proxy")
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.setProxy(Integer.parseInt(s))),
                        CommandOption.of("cn", "Cname (default " + settings.getSSL().getCname())
                                .withLong("cname")
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.getSSL().setDer(s)),
                        CommandOption.of("der", "Der file (default " + settings.getSSL().getDer())
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.getSSL().setDer(s)),
                        CommandOption.of("key", "Key file (default " + settings.getSSL().getKey())
                                .withMandatoryParameter()
                                .withCallback((s) -> settings.getSSL().setKey(s))

                )

        );


        return options;
    }

    @Override
    protected String getConnectionDescription() {
        return "http://localhost";
    }


    @Override
    protected String getDefaultPort() {
        return "80";
    }

    @Override
    public String getId() {
        return "http";
    }

    @Override
    public String getDescription() {
        return "Http Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new HttpProtocolSettings();
    }
}
