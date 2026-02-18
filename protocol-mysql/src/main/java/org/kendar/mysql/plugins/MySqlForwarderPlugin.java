package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.settings.PluginSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.kendar.plugins.base.ProtocolPhase.*;

@TpmService(tags = "mysql")
public class MySqlForwarderPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    private static Object lock = new Object();
    private static ConcurrentHashMap<String,Boolean> activeProfiles = new ConcurrentHashMap<>();
    private static AtomicBoolean registered = new AtomicBoolean(false);
    public MySqlForwarderPlugin(JsonMapper mapper) {
        super(mapper);
        if(registered.get())return;
        synchronized (lock){
            if(registered.get())return;
            registered.set(true);
            EventsQueue.register(UUID.randomUUID().toString(), MySqlForwarderPlugin::handleConnect, MysqlConnect.class);
        }
    }

    private static void handleConnect(MysqlConnect e) {
        var ctx = e.getProtoContext();
        var instanceId = ctx.getDescriptor().getSettings().getProtocolInstanceId();
        if(instanceId==null)instanceId="default";
        if(activeProfiles.containsKey(instanceId)){
            var jdbcProxy = (JdbcProxy)ctx.getProxy();


            var userid = ctx.getValue("userid","");
            var database = ctx.getValue("database","");
            var password = ctx.getValue("password","");
            var connectionString = jdbcProxy.getConnectionString();
            if(password.trim().isEmpty() || userid.trim().isEmpty()){
                return;
            }

            try {
                var uri = new URI(connectionString.substring(5));
                var newConnectionString = "jdbc:" + uri.getScheme() + "://" + uri.getHost();
                if (uri.getPort() > 0) {
                    newConnectionString += ":" + uri.getPort();
                }
                var originalDatabase = uri.getPath().substring(1);
                if (!database.isEmpty()) {
                    newConnectionString += "/" + database;
                } else if (uri.getPath().length() > 1) {
                    newConnectionString += "/" + uri.getPath().substring(1);
                    ;
                }

                var params = parseQuery(uri.getQuery());
                var entrySet = params.entrySet();
                for (var entry : entrySet) {
                    if (entry.getKey().equalsIgnoreCase("user") || entry.getKey().equalsIgnoreCase("password")) {
                        params.remove(entry.getKey());
                    } else {
                        newConnectionString += (newConnectionString.contains("?") ? "&" : "?") + entry.getKey() + "=" + entry.getValue();
                    }
                }

                var connection = DriverManager.
                        getConnection(newConnectionString, userid, password);
                log.error("Override connection String " +newConnectionString);
                var conn = new ProxyConnection(connection);
                ctx.setValue("CONNECTION", conn);
            }catch (Exception ex){
                ctx.setValue("CONNECTION", null);
                log.error("Error connecting to database",ex);
                throw new RuntimeException("Error connecting to database",ex);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MySqlForwarderPlugin.class);

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            map.put(parts[0], parts.length > 1 ? parts[1] : "");
        }
        return map;
    }

    @Override
    protected void handleActivation(boolean active) {
        if(active){
            activeProfiles.put(getInstanceId(),true);
        }else{
            activeProfiles.remove(getInstanceId());
        }
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(NONE);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }

    @Override
    public String getId() {
        return "mysql-forwarder";
    }
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        return false;
    }
}
