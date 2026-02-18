package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.JdbcConnect;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.settings.JdbcRewritePluginSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.kendar.plugins.base.ProtocolPhase.NONE;

public abstract class BasicJdbcForwardPlugin extends ProtocolPluginDescriptorBase<JdbcRewritePluginSettings> {
    private static Object lock = new Object();
    private static ConcurrentHashMap<String, BasicJdbcForwardPlugin> activeProfiles = new ConcurrentHashMap<>();
    private static AtomicBoolean registered = new AtomicBoolean(false);
    public BasicJdbcForwardPlugin(JsonMapper mapper) {
        super(mapper);
        if(registered.get())return;
        synchronized (lock){
            if(registered.get())return;
            registered.set(true);
            EventsQueue.register(UUID.randomUUID().toString(), BasicJdbcForwardPlugin::handleConnect, JdbcConnect.class);
        }
    }

    private AtomicReference<List<JdbcForwardMatcher>> matchers = new AtomicReference<>(new ArrayList<>());
    @Override
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        matchers.set(setupMatches(getSettings().getMappings()));
        return true;
    }

    private List<JdbcForwardMatcher> setupMatches(HashMap<String, String> mappings) {
        var result = new ArrayList<JdbcForwardMatcher>();
        for(var item:mappings.entrySet()){
            result.add(new JdbcForwardMatcher(item.getKey(),item.getValue()));
        }
        return result;
    }
    
    public List<JdbcForwardMatcher> getMatchers(){
        return this.matchers.get();
    }

    private static void handleConnect(JdbcConnect e) {
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
                var pluginInstance = activeProfiles.get(instanceId);
                var mathchersList  = pluginInstance.matchers.get();
                
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
                }

                String matched = null;
                for(var i=0;i<mathchersList.size();i++){
                    var matcher = mathchersList.get(i);
                    matched = matcher.match(newConnectionString );
                    if(matched!=null)break;
                }
                if(matched==null)return;
                newConnectionString = matched;

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

    private static final Logger log = LoggerFactory.getLogger(BasicJdbcForwardPlugin.class);

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
            activeProfiles.put(getInstanceId(),this);
        }else{
            activeProfiles.remove(getInstanceId());
        }
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(NONE);
    }


    @Override
    public String getId() {
        return "jdbc-forward";
    }
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        return false;
    }
}
