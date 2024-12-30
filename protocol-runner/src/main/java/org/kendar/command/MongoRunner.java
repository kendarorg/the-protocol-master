package org.kendar.command;

import org.kendar.annotations.di.TpmService;
import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.mongo.MongoProxy;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
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
@TpmService
public class MongoRunner extends CommonRunner {
    private TcpServer ps;

    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new ByteProtocolSettingsWithLogin();
        settings.setProtocol(getId());
        var recording = new BasicRecordPluginSettings();
        var replaying = new BasicReplayPluginSettings();
        List<CommandOption> commandOptionList = getCommonOptions(
                globalSettings, settings, recording, replaying, optionLoginPassword(settings));
        return CommandOptions.of(getId())
                .withDescription("MongoDb Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }

    @Override
    public String getId() {
        return "mongodb";
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
    public String getDefaultPort() {
        return "27018";
    }

    @Override
    protected String getConnectionDescription() {
        return "mongodb://localhost:27018";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer,
                      String key, GlobalSettings ini,
                      ProtocolSettings protocol, StorageRepository storage,
                      List<ProtocolPluginDescriptor> plugins, Supplier<Boolean> stopWhenFalse) throws Exception {

        var protocolSettings = (ByteProtocolSettingsWithLogin) protocol;

        var port = ProtocolsRunner.getOrDefault(protocolSettings.getPort(), 1883);
        var timeoutSec = ProtocolsRunner.getOrDefault(protocolSettings.getTimeoutSeconds(), 30);
        var connectionString = ProtocolsRunner.getOrDefault(protocolSettings.getConnectionString(), "");
        var login = ProtocolsRunner.getOrDefault(protocolSettings.getLogin(), "");
        var password = ProtocolsRunner.getOrDefault(protocolSettings.getPassword(), "");
        var baseProtocol = new org.kendar.mqtt.MqttProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MongoProxy(connectionString);
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            var specificPluginSetting = protocol.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (specificPluginSetting != null) {
                plugin.initialize(ini, protocolSettings, specificPluginSetting);
                plugin.refreshStatus();
            } else {
                plugins.remove(i);
            }
        }
        proxy.setPlugins(plugins);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        ps = new TcpServer(baseProtocol);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }
}
