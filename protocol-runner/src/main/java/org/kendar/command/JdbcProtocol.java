package org.kendar.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.kendar.filters.FilterDescriptor;
import org.kendar.mysql.MySQLProtocol;
import org.kendar.mysql.MySqlStorageHandler;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcStorageHandler;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JdbcProtocol extends CommonProtocol{
    private final String protocol;

    public JdbcProtocol(String id){

        this.protocol = id;
    }


    @Override
    public String getDefaultPort() {
        return protocol.equalsIgnoreCase("mysql") ? "3306" : "5432";
    }

    public void run(String[] args, boolean isExecute, Ini go, Options mainOptions) throws Exception {

        var options=getCommonOptions( mainOptions);
        optionLoginPassword(options);
        options.addOption("schema", true, "Set schema");
        options.addOption("replaceQueryFile", true, "Replace queries file");
        if(!isExecute)return;
        setCommonData(args,options,go);
    }
    protected void parseExtra(Ini result, CommandLine cmd){
        var section = "["+cmd.getOptionValue("protocol")+"]";
        parseLoginPassword(result, cmd, section);
        result.putValue(section,"schema",cmd.getOptionValue("schema"));
        result.putValue(section,"replaceQueryFile",cmd.getOptionValue("replaceQueryFile"));
    }

    @Override
    public String getId() {
        return protocol;
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

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String key, Ini ini, String protocol, StorageRepository repo, ArrayList<FilterDescriptor> filters, Supplier<Boolean> stopWhenFalse) throws Exception {
        NetworkProtoDescriptor baseProtocol = null;
        String driver = "";
        if(protocol.equalsIgnoreCase("postgres")) {
            driver = "org.postgresql.Driver";
            baseProtocol = new PostgresProtocol(ini.getValue(key,"port",Integer.class));
        }else if(protocol.equalsIgnoreCase("mysql")) {
            baseProtocol = new MySQLProtocol(ini.getValue(key,"port",Integer.class));
        }

        var proxy = new JdbcProxy(driver,
                ini.getValue(key,"connection",String.class), ini.getValue(key,"schema",String.class),
                ini.getValue(key,"login",String.class), ini.getValue(key,"password",String.class));

        JdbcStorageHandler storage = new JdbcStorageHandler(repo);
        if (protocol.equalsIgnoreCase("mysql")) {
            storage = new MySqlStorageHandler(repo);
        }
        if (ini.getValue(key,"replay",Boolean.class)) {
            proxy = new JdbcProxy(storage);
        } else {
            proxy.setStorage(storage);
        }
        proxy.setFilters(filters);
        var jdbcReplaceQueries = ini.getValue(key,"replaceQueryFile",String.class);
        if (jdbcReplaceQueries != null && !jdbcReplaceQueries.isEmpty() && Files.exists(Path.of(jdbcReplaceQueries))) {

            handleReplacementQueries(jdbcReplaceQueries, proxy);
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.setTimeout(ini.getValue(key,"timeout",Integer.class));
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(ini.getValue(key,"respectcallduration",Boolean.class));
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key,ps);
    }
    
}


