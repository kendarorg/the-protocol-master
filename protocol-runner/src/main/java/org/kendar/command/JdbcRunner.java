package org.kendar.command;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.mysql.MySQLProtocol;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.RewritePluginSettings;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.ReplacerItem;
import org.kendar.utils.Sleeper;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JdbcRunner extends CommonRunner {
    private static final TypeReference<List<ReplacerItem>> replaceItemsList = new TypeReference<>() {
    };
    private static final JsonMapper mapper = new JsonMapper();
    private final String protocol;
    private TcpServer ps;

    public JdbcRunner(String id) {

        this.protocol = id;
    }



    @Override
    public String getDefaultPort() {
        return protocol.equalsIgnoreCase("mysql") ? "3306" : "5432";
    }

    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go, Options mainOptions,
                    HashMap<String, List<PluginDescriptor>> filters) throws Exception {
        var options = getCommonOptions(mainOptions);
        optionLoginPassword(options);
        options.addOption(createOpt("js", "schema", true, "Set schema"));
        options.addOption(createOpt("rew", "rewrite", true, "Rewrite a query (requires a file)."));
        if (!isExecute) return;
        setCommonData(args, options, go, new JdbcProtocolSettings());
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin) result, cmd);
        var sets = (JdbcProtocolSettings) result;

        sets.setForceSchema(ProtocolsRunner.getOrDefault(cmd.getOptionValue("schema"), ""));
        if(cmd.hasOption("rewrite")){
            var pl = new RewritePluginSettings();
            pl.setRewritesFile(cmd.getOptionValue("rewrite","rewrite.json"));
            sets.getPlugins().put("rewrite-plugin", pl);
        }
    }

    @Override
    public String getId() {
        return protocol;
    }

    @Override
    public Class<?> getSettingsClass() {
        return JdbcProtocolSettings.class;
    }

    @Override
    public void stop() {
        ps.stop();
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                      GlobalSettings ini, ProtocolSettings protocolSettings,
                      StorageRepository repo, List<PluginDescriptor> plugins,
                      Supplier<Boolean> stopWhenFalse) throws Exception {
        NetworkProtoDescriptor baseProtocol = null;
        var realSttings = (JdbcProtocolSettings) protocolSettings;
        String driver = "";
        if (protocolSettings.getProtocol().equalsIgnoreCase("postgres")) {
            driver = "org.postgresql.Driver";
            baseProtocol = new PostgresProtocol(
                    ProtocolsRunner.getOrDefault(realSttings.getPort(), 5432)
            );
        } else if (protocolSettings.getProtocol().equalsIgnoreCase("mysql")) {
            driver = "com.mysql.cj.jdbc.Driver";
            baseProtocol = new MySQLProtocol(
                    ProtocolsRunner.getOrDefault(realSttings.getPort(), 3306));
        }

        var proxy = new JdbcProxy(driver,
                realSttings.getConnectionString(), realSttings.getForceSchema(),
                realSttings.getLogin(), realSttings.getPassword());
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            plugin.initialize(ini, protocolSettings);
        }
        proxy.setPlugins(plugins);

        baseProtocol.setProxy(proxy);
        baseProtocol.setTimeout(ProtocolsRunner.getOrDefault(realSttings.getTimeoutSeconds(), 30));
        baseProtocol.initialize();
        ps = new TcpServer(baseProtocol);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

}


