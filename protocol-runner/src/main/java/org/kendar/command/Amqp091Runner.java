package org.kendar.command;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Amqp091Runner extends CommonRunner {
    private TcpServer ps;


    @Override
    protected String getConnectionDescription() {
        return "amqp://localhost:5372";
    }


    @Override
    public String getDefaultPort() {
        return "5672";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServers,
                      String key, GlobalSettings ini,
                      ProtocolSettings opaqueProtocolSettings,
                      StorageRepository storage,
                      List<PluginDescriptor> plugins, Supplier<Boolean> stopWhenFalse) throws Exception {
        var protocolSettings = (ByteProtocolSettingsWithLogin) opaqueProtocolSettings;

        var port = ProtocolsRunner.getOrDefault(protocolSettings.getPort(), 5672);
        var timeoutSec = ProtocolsRunner.getOrDefault(protocolSettings.getTimeoutSeconds(), 30);
        var connectionString = ProtocolsRunner.getOrDefault(protocolSettings.getConnectionString(), "");
        var login = ProtocolsRunner.getOrDefault(protocolSettings.getLogin(), "");
        var password = ProtocolsRunner.getOrDefault(protocolSettings.getPassword(), "");
        var baseProtocol = new org.kendar.amqp.v09.AmqpProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new AmqpProxy(connectionString, login, password);

        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            var specificPluginSetting = opaqueProtocolSettings.getPlugin(plugin.getId(), plugin.getSettingClass());
            plugin.initialize(ini, opaqueProtocolSettings, specificPluginSetting);
            plugin.refreshStatus();
        }
        proxy.setPlugins(plugins);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        ps = new TcpServer(baseProtocol);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServers.put(key, ps);
    }

    @Override
    public String getId() {
        return "amqp091";
    }

    @Override
    public Class<?> getSettingsClass() {
        return ByteProtocolSettingsWithLogin.class;
    }

    @Override
    public void stop() {
        ps.stop();
    }

    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new ByteProtocolSettingsWithLogin();
        settings.setProtocol(getId());
        var recording = new BasicRecordPluginSettings();
        var replaying = new BasicReplayPluginSettings();
        List<CommandOption> commandOptionList = getCommonOptions(globalSettings, settings, recording, replaying, optionLoginPassword(settings));
        return CommandOptions.of(getId())
                .withDescription("Amqp 0.9.1 Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }
}
