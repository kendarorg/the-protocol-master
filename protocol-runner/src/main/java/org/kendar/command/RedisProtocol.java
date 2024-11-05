package org.kendar.command;

import org.apache.commons.cli.Options;
import org.kendar.filters.FilterDescriptor;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.Resp3StorageHandler;
import org.kendar.server.TcpServer;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RedisProtocol extends CommonProtocol{
    @Override
    public void run(String[] args, boolean isExecute, Ini go, Options mainOptions) throws Exception {
        var options=getCommonOptions(mainOptions);
        if(!isExecute)return;
        setCommonData(args,options,go);
    }

    @Override
    public String getDefaultPort() {
        return "6379";
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, Ini ini, String protocol, StorageRepository storage, ArrayList<FilterDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception {
        var port =ini.getValue(key,"port",Integer.class);
        var timeoutSec =ini.getValue(key,"timeout",Integer.class);
        var connectionString =ini.getValue(key,"connection",String.class);
        var login =ini.getValue(key,"login",String.class);
        var password =ini.getValue(key,"password",String.class);
        var baseProtocol = new Resp3Protocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new Resp3Proxy(connectionString, login, password);

        if (ini.getValue(key,"replay",Boolean.class)) {
            proxy = new Resp3Proxy();
            proxy.setStorage(new Resp3StorageHandler(storage) {
            });
        } else {
            proxy.setStorage(new Resp3StorageHandler(storage));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(ini.getValue(key,"respectcallduration",Boolean.class));
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key,ps);
    }

    @Override
    public String getId() {
        return "redis";
    }


}
