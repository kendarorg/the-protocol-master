package org.kendar.command;

import org.apache.commons.cli.Options;
import org.kendar.filters.PluginDescriptor;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.Resp3StorageHandler;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RedisRunner extends CommonRunner {
    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go, Options mainOptions,
                    HashMap<String, List<PluginDescriptor>> filters) throws Exception {
        var options = getCommonOptions(mainOptions);
        if (!isExecute) return;
        setCommonData(args, options, go, new ByteProtocolSettings());
    }

    @Override
    public String getDefaultPort() {
        return "6379";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                      GlobalSettings ini, ProtocolSettings protocol,
                      StorageRepository storage, List<PluginDescriptor> filters,
                      Supplier<Boolean> stopWhenFalse) throws Exception {
        var protocolSettings = (ByteProtocolSettings) protocol;
        var port = ProtocolsRunner.getOrDefault(protocolSettings.getPort(), 6379);
        var timeoutSec = ProtocolsRunner.getOrDefault(protocolSettings.getTimeoutSeconds(), 30);
        var connectionString = ProtocolsRunner.getOrDefault(protocolSettings.getConnectionString(), "");
        var baseProtocol = new Resp3Protocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new Resp3Proxy(connectionString, null, null);

        if (protocolSettings.getSimulation() != null && protocolSettings.getSimulation().isReplay()) {
            proxy = new Resp3Proxy();
            proxy.setStorage(new Resp3StorageHandler(storage));
        } else {
            proxy.setStorage(new Resp3StorageHandler(storage));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        if (protocolSettings.getSimulation() != null && protocolSettings.getSimulation().isReplay()) {
            ps.useCallDurationTimes(protocolSettings.getSimulation().isRespectCallDuration());
        }
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

    @Override
    public String getId() {
        return "redis";
    }

    @Override
    public Class<?> getSettingsClass() {
        return ByteProtocolSettings.class;
    }


}
