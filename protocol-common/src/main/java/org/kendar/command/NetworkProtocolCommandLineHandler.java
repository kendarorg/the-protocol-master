package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;

public abstract class NetworkProtocolCommandLineHandler extends ProtocolCommandLineHandler {

    @Override
    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        var options = new ArrayList<CommandOption>();
        if (ByteProtocolSettings.class.isAssignableFrom(genericSettings.getClass())) {
            var settings = (ByteProtocolSettings) genericSettings;
            settings.setPort(Integer.parseInt(getDefaultPort()));
            options.addAll(List.of(
                    CommandOption.of("po", "Listening port (default " + getDefaultPort() + ")")
                            .withLong("port")
                            .withMandatoryParameter()
                            .withCallback((s) -> settings.setPort(Integer.parseInt(s))),
                    CommandOption.of("pc", "Connection (example " + getConnectionDescription() + ")")
                            .withLong("connection")
                            .withMandatoryParameter()
                            .withCallback(settings::setConnectionString),
                    CommandOption.of("pt", "Timeout (deafult " + settings.getTimeoutSeconds() + ")")
                            .withLong("timeout")
                            .withMandatoryParameter()
                            .withCallback((s) -> settings.setTimeoutSeconds(Integer.parseInt(s)))));
        }
        if (ByteProtocolSettingsWithLogin.class.isAssignableFrom(genericSettings.getClass())) {
            var settings = (ByteProtocolSettingsWithLogin) genericSettings;
            options.addAll(List.of(
                    CommandOption.of("pu", "Remote login")
                            .withLong("login")
                            .withMandatoryParameter()
                            .withCallback(settings::setLogin),

                    CommandOption.of("pw", "Remote password")
                            .withLong("password")
                            .withMandatoryParameter()
                            .withCallback(settings::setPassword)));
        }
        options.addAll(super.prepareCustomOptions(globalSettings, genericSettings));
        return options;
    }

    protected abstract String getConnectionDescription();

    protected abstract String getDefaultPort();
}
