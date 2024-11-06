package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.filters.PluginDescriptor;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.MqttStorageHandler;
import org.kendar.server.TcpServer;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MqttProtocol extends CommonProtocol {
    @Override
    public void run(String[] args, boolean isExecute, Ini go, Options mainOptions) throws Exception {
        var options = getCommonOptions(mainOptions);
        optionLoginPassword(options);
        if (!isExecute) return;
        setCommonData(args, options, go);
    }

    protected void parseExtra(Ini result, CommandLine cmd) {
        var section = cmd.getOptionValue("protocol");
        parseLoginPassword(result, cmd, section);
    }

    @Override
    public String getId() {
        return "mqtt";
    }

    @Override
    public String getDefaultPort() {
        return "1883";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, Ini ini, String protocol, StorageRepository storage, ArrayList<PluginDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception {
        var port = ini.getValue(key, "port", Integer.class, 1883);
        var timeoutSec = ini.getValue(key, "timeout", Integer.class, 30);
        var connectionString = ini.getValue(key, "connection", String.class);
        var login = ini.getValue(key, "login", String.class);
        var password = ini.getValue(key, "password", String.class);
        var baseProtocol = new org.kendar.mqtt.MqttProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MqttProxy(connectionString, login, password);

        if (ini.getValue(key, "replay", Boolean.class, false)) {
            proxy = new MqttProxy();
            proxy.setStorage(new MqttStorageHandler(storage) {
            });
        } else {
            proxy.setStorage(new MqttStorageHandler(storage));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(ini.getValue(key, "respectcallduration", Boolean.class, false));
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

}
