package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.filters.PluginDescriptor;
import org.kendar.mysql.MySQLProtocol;
import org.kendar.mysql.MySqlStorageHandler;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.JdbcProtocolSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcStorageHandler;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JdbcProtocol extends CommonProtocol {
    private final String protocol;

    public JdbcProtocol(String id) {

        this.protocol = id;
    }

    private static void handleReplacementQueries(String jdbcReplaceQueries, JdbcProxy proxy) throws Exception {
        var lines = Files.readAllLines(Path.of(jdbcReplaceQueries));
        var items = new ArrayList<QueryReplacerItem>();
        QueryReplacerItem replacerItem = new QueryReplacerItem();
        boolean find = false;
        for (var line : lines) {
            if (line.toLowerCase().startsWith("#regexfind")) {
                if (replacerItem.getToFind() != null) {
                    items.add(replacerItem);
                    replacerItem = new QueryReplacerItem();
                }
                replacerItem.setRegex(true);
                replacerItem.setToFind("");
                find = true;
            } else if (line.toLowerCase().startsWith("#find")) {
                if (replacerItem.getToFind() != null) {
                    items.add(replacerItem);
                    replacerItem = new QueryReplacerItem();
                }
                replacerItem.setToFind("");
                find = true;
            } else if (line.toLowerCase().startsWith("#replace")) {
                replacerItem.setToReplace("");
                find = false;
            } else {
                if (find) {
                    replacerItem.setToFind(replacerItem.getToFind() + line + "\n");
                } else {
                    replacerItem.setToReplace(replacerItem.getToReplace() + line + "\n");
                }
            }
        }
        if (replacerItem.getToFind() != null && !replacerItem.getToFind().isEmpty()) {
            items.add(replacerItem);
        }
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
        options.addOption(createOpt("js","schema", true, "Set schema"));
        options.addOption(createOpt("jr","replaceQueryFile", true, "Replace queries file"));
        if (!isExecute) return;
        setCommonData(args, options, go, new JdbcProtocolSettings());
    }

    protected void parseExtra(ByteProtocolSettings result, CommandLine cmd) {
        parseLoginPassword((ByteProtocolSettingsWithLogin)result, cmd);
        var sets= (JdbcProtocolSettings)result;

        sets.setForceSchema(OptionsManager.getOrDefault(cmd.getOptionValue("schema"),""));
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
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key,
                      GlobalSettings ini, ProtocolSettings protocolSettings,
                      StorageRepository repo, List<PluginDescriptor> filters,
                      Supplier<Boolean> stopWhenFalse) throws Exception {
        NetworkProtoDescriptor baseProtocol = null;
        var realSttings = (JdbcProtocolSettings)protocolSettings;
        String driver = "";
        if (protocolSettings.getProtocol().equalsIgnoreCase("postgres")) {
            driver = "org.postgresql.Driver";
            baseProtocol = new PostgresProtocol(
                    OptionsManager.getOrDefault(realSttings.getPort(),5432)
            );
        } else if (protocolSettings.getProtocol().equalsIgnoreCase("mysql")) {
            baseProtocol = new MySQLProtocol(
                    OptionsManager.getOrDefault(realSttings.getPort(),3306));
        }

        var proxy = new JdbcProxy(driver,
                realSttings.getConnectionString(), realSttings.getForceSchema(),
                realSttings.getLogin(), realSttings.getPassword());

        JdbcStorageHandler storage = new JdbcStorageHandler(repo);
        if (protocolSettings.getProtocol().equalsIgnoreCase("mysql")) {
            storage = new MySqlStorageHandler(repo);
        }
        if (realSttings.getSimulation()!=null && realSttings.getSimulation().isReplay()) {
            proxy = new JdbcProxy(storage);
        } else {
            proxy.setStorage(storage);
        }
        proxy.setFilters(filters);
//        var jdbcReplaceQueries = ini.getValue(key, "replaceQueryFile", String.class, null);
//        if (jdbcReplaceQueries != null && !jdbcReplaceQueries.isEmpty() && Files.exists(Path.of(jdbcReplaceQueries))) {
//
//            handleReplacementQueries(jdbcReplaceQueries, proxy);
//        }
        baseProtocol.setProxy(proxy);
        baseProtocol.setTimeout(OptionsManager.getOrDefault(realSttings.getTimeoutSeconds(),30));
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        if (realSttings.getSimulation()!=null && realSttings.getSimulation().isReplay()) {
            ps.useCallDurationTimes(realSttings.getSimulation().isRespectCallDuration());
        }
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

}


