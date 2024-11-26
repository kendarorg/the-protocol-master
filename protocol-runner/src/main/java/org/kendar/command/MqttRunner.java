package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.mqtt.MqttProxy;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MqttRunner extends CommonRunner {
    private TcpServer ps;

    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go,
                    Options mainOptions, HashMap<String, List<PluginDescriptor>> filters) throws Exception {
        var options = getCommonOptions(mainOptions);
        optionLoginPassword(options);
        if (!isExecute) return;
        setCommonData(args, options, go, new ByteProtocolSettingsWithLogin());
    }

    @Override
    protected String getConnectionDescription() {
        return "tcp://localhost:1884";
    }

    @Override
    public String getDefaultPort() {
        return "1883";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServers,
                      String key, GlobalSettings ini,
                      ProtocolSettings opaqueProtocolSettings,
                      StorageRepository storage,
                      List<PluginDescriptor> plugins, Supplier<Boolean> stopWhenFalse) throws Exception {
        var protocolSettings = (ByteProtocolSettingsWithLogin) opaqueProtocolSettings;
        var port = ProtocolsRunner.getOrDefault(protocolSettings.getPort(), 1883);
        var timeoutSec = ProtocolsRunner.getOrDefault(protocolSettings.getTimeoutSeconds(), 30);
        var connectionString = ProtocolsRunner.getOrDefault(protocolSettings.getConnectionString(), "");
        var login = ProtocolsRunner.getOrDefault(protocolSettings.getLogin(), "");
        var password = ProtocolsRunner.getOrDefault(protocolSettings.getPassword(), "");
        var baseProtocol = new org.kendar.mqtt.MqttProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MqttProxy(connectionString, login, password);
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            var specificPluginSetting = opaqueProtocolSettings.getPlugin(plugin.getId(), plugin.getSettingClass());
            plugin.initialize(ini, protocolSettings, specificPluginSetting);
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
        return "mqtt";
    }

    @Override
    public Class<?> getSettingsClass() {
        return ByteProtocolSettings.class;
    }

    @Override
    public void stop() {
        ps.stop();
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin) result, cmd);
    }
}
