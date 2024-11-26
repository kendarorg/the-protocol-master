package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.amqp.v09.AmqpProxy;
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

public class Amqp091Runner extends CommonRunner {


    private TcpServer ps;

    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go,
                    Options mainOptions, HashMap<String, List<PluginDescriptor>> filters) throws Exception {

        var options = getCommonOptions(mainOptions);
        optionLoginPassword(options);
        if (!isExecute) return;
        setCommonData(args, options, go, new ByteProtocolSettings());
    }


    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin) result, cmd);
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
    public String getDefaultPort() {
        return "5672";
    }

    @Override
    protected String getConnectionDescription() {
        return "amqp://localhost:5372";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, GlobalSettings ini, ProtocolSettings protocol, StorageRepository storage, List<PluginDescriptor> plugins, Supplier<Boolean> stopWhenFalse) throws Exception {

        var protocolSettings = (ByteProtocolSettingsWithLogin) protocol;

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
            var specificPluginSetting = protocol.getPlugin(plugin.getId(), plugin.getSettingClass());
            plugin.initialize(ini, protocol,specificPluginSetting);
            plugin.refreshStatus();
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
