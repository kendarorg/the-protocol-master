package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.mongo.MongoProxy;
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

public class MongoRunner extends CommonRunner {
    private TcpServer ps;

    public void run(String[] args, boolean isExecute, GlobalSettings go,
                    Options mainOptions, HashMap<String, List<PluginDescriptor>> filters) throws Exception {

        var options = getCommonOptions(mainOptions);
        optionLoginPassword(options);
        if (!isExecute) return;
        setCommonData(args, options, go, new ByteProtocolSettingsWithLogin());
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin) result, cmd);
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
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, GlobalSettings ini,
                      ProtocolSettings protocol, StorageRepository storage,
                      List<PluginDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception {

        var protocolSettings = (ByteProtocolSettingsWithLogin) protocol;

        var port = ProtocolsRunner.getOrDefault(protocolSettings.getPort(), 1883);
        var timeoutSec = ProtocolsRunner.getOrDefault(protocolSettings.getTimeoutSeconds(), 30);
        var connectionString = ProtocolsRunner.getOrDefault(protocolSettings.getConnectionString(), "");
        var login = ProtocolsRunner.getOrDefault(protocolSettings.getLogin(), "");
        var password = ProtocolsRunner.getOrDefault(protocolSettings.getPassword(), "");
        var baseProtocol = new org.kendar.mqtt.MqttProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MongoProxy(connectionString);


        proxy.setPlugins(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        ps = new TcpServer(baseProtocol);

        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

}
