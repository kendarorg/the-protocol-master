package org.kendar.command;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.mysql.MySQLProtocol;
import org.kendar.plugins.PluginDescriptor;
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
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JdbcRunner extends CommonRunner {
    private static TypeReference<List<QueryReplacerItem>> replaceItemsList = new TypeReference<>() {
    };
    private static JsonMapper mapper = new JsonMapper();
    private final String protocol;
    private TcpServer ps;

    public JdbcRunner(String id) {

        this.protocol = id;
    }

    private static void handleReplacementQueries(String jdbcReplaceQueries, JdbcProxy proxy) throws Exception {
        var lines = new String(Files.readAllBytes(Path.of(jdbcReplaceQueries).toAbsolutePath()));

        var items = (List<QueryReplacerItem>) mapper.deserialize(lines, replaceItemsList);

        proxy.setQueryReplacement(items);
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
        options.addOption(createOpt("jr", "replaceQueryFile", true, "Replace queries file"));
        if (!isExecute) return;
        setCommonData(args, options, go, new JdbcProtocolSettings());
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin) result, cmd);
        var sets = (JdbcProtocolSettings) result;

        sets.setForceSchema(ProtocolsRunner.getOrDefault(cmd.getOptionValue("schema"), ""));
        sets.setReplaceQueryFile(ProtocolsRunner.getOrDefault(cmd.getOptionValue("replaceQueryFile"), ""));
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
                      StorageRepository repo, List<PluginDescriptor> filters,
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
            baseProtocol = new MySQLProtocol(
                    ProtocolsRunner.getOrDefault(realSttings.getPort(), 3306));
        }

        var proxy = new JdbcProxy(driver,
                realSttings.getConnectionString(), realSttings.getForceSchema(),
                realSttings.getLogin(), realSttings.getPassword());

        proxy.setPlugins(filters);
        var jdbcReplaceQueries = ((JdbcProtocolSettings) protocolSettings).getReplaceQueryFile();
        if (jdbcReplaceQueries != null && !jdbcReplaceQueries.isEmpty() && Files.exists(Path.of(jdbcReplaceQueries))) {

            handleReplacementQueries(jdbcReplaceQueries, proxy);
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.setTimeout(ProtocolsRunner.getOrDefault(realSttings.getTimeoutSeconds(), 30));
        baseProtocol.initialize();
        ps = new TcpServer(baseProtocol);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

}


