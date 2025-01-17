package org.kendar.command;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.di.DiService;
import org.kendar.mysql.MySQLProtocol;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.tcpserver.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.ReplacerItem;
import org.kendar.utils.Sleeper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JdbcRunner extends CommonRunner {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final TypeReference<List<ReplacerItem>> replaceItemsList = new TypeReference<>() {
    };
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
    protected String getConnectionDescription() {
        return protocol.equalsIgnoreCase("mysql") ? "jdbc:mysql://localhost:3306" :
                "jdbc:postgresql://localhost:5432/db?ssl=false";
    }

    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new JdbcProtocolSettings();
        settings.setProtocol(getId());
        var recording = new BasicRecordPluginSettings();
        var replaying = new BasicReplayPluginSettings();
        var rewrite = new RewritePluginSettings();
        var extra = new ArrayList<CommandOption>();
        extra.addAll(optionLoginPassword(settings));
        extra.addAll(List.of(
                CommandOption.of("js", "Force schema name")
                        .withLong("schema")
                        .withMandatoryParameter()
                        .withCallback(settings::setForceSchema),
                CommandOption.of("rew", "Path of the rewrite queries file")
                        .withLong("rewrite")
                        .withMandatoryParameter()
                        .withCallback((s) -> {
                            settings.getPlugins().put("rewrite-plugin", rewrite);
                            rewrite.setActive(true);
                            rewrite.setRewritesFile(s);
                        })
        ));
        List<CommandOption> commandOptionList = getCommonOptions(globalSettings, settings, recording, replaying, extra);
        return CommandOptions.of(getId())
                .withDescription(getId() + " Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
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
                      StorageRepository repo, List<ProtocolPluginDescriptor> plugins,
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
            var specificPluginSetting = protocolSettings.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (specificPluginSetting != null) {
                plugin.initialize(ini, protocolSettings, specificPluginSetting);
                plugin.refreshStatus();
            } else {
                plugins.remove(i);
            }
        }
        proxy.setPlugins(plugins);

        baseProtocol.setProxy(proxy);
        baseProtocol.setTimeout(ProtocolsRunner.getOrDefault(realSttings.getTimeoutSeconds(), 30));
        baseProtocol.initialize();
        var diService = DiService.getThreadContext();
        ps = new TcpServer(baseProtocol);
        ps.setOnStart(() -> {
            DiService.setThreadContext(diService);
        });
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(key, ps);
    }

}


