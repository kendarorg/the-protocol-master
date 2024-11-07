package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.AmqpStorageHandler;
import org.kendar.filters.PluginDescriptor;
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

public class Amqp091Protocol extends CommonProtocol {


    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go,
                    Options mainOptions, HashMap<String, List<PluginDescriptor>> filters) throws Exception {

        var options = getCommonOptions(mainOptions);
        optionLoginPassword(options);
        if (!isExecute) return;
        setCommonData(args, options, go,new ByteProtocolSettings());
    }


    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin)result, cmd);
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
    public String getDefaultPort() {
        return "5672";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, GlobalSettings ini, ProtocolSettings protocol, StorageRepository storage, List<PluginDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception {

        var protocolSettings = (ByteProtocolSettingsWithLogin) protocol;

        var port = OptionsManager.getOrDefault(protocolSettings.getPort(), 5672);
        var timeoutSec = OptionsManager.getOrDefault(protocolSettings.getTimeoutSeconds(),30);
        var connectionString = OptionsManager.getOrDefault(protocolSettings.getConnectionString(),"");
        var login = OptionsManager.getOrDefault(protocolSettings.getLogin(),"");
        var password = OptionsManager.getOrDefault(protocolSettings.getPassword(),"");
        var baseProtocol = new org.kendar.amqp.v09.AmqpProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new AmqpProxy(connectionString, login, password);

        if (protocolSettings.getSimulation()!=null && protocolSettings.getSimulation().isReplay()) {
            proxy = new AmqpProxy();
            proxy.setStorage(new AmqpStorageHandler(storage));
        } else {
            proxy.setStorage(new AmqpStorageHandler(storage));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        if (protocolSettings.getSimulation()!=null && protocolSettings.getSimulation().isReplay()) {
            ps.useCallDurationTimes(protocolSettings.getSimulation().isRespectCallDuration());
        }
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }
}
