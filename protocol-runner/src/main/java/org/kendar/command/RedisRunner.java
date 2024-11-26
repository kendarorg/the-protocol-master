package org.kendar.command;

import org.apache.commons.cli.Options;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
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
    private TcpServer ps;

    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go, Options mainOptions,
                    HashMap<String, List<PluginDescriptor>> filters) throws Exception {
        var options = getCommonOptions(mainOptions);
        if (!isExecute) return;
        setCommonData(args, options, go, new ByteProtocolSettings());
    }

    @Override
    protected String getConnectionDescription() {
        return "redis://localhost:5372";
    }

    @Override
    public String getDefaultPort() {
        return "6379";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServers, String key,
                      GlobalSettings ini, ProtocolSettings opaqueProtocolSettings,
                      StorageRepository storage, List<PluginDescriptor> plugins,
                      Supplier<Boolean> stopWhenFalse) throws Exception {
        var protocolSettings = (ByteProtocolSettings) opaqueProtocolSettings;
        var port = ProtocolsRunner.getOrDefault(protocolSettings.getPort(), 6379);
        var timeoutSec = ProtocolsRunner.getOrDefault(protocolSettings.getTimeoutSeconds(), 30);
        var connectionString = ProtocolsRunner.getOrDefault(protocolSettings.getConnectionString(), "");
        var baseProtocol = new Resp3Protocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new Resp3Proxy(connectionString, null, null);
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
        return "redis";
    }

    @Override
    public Class<?> getSettingsClass() {
        return ByteProtocolSettings.class;
    }

    @Override
    public void stop() {
        ps.stop();
    }


}
