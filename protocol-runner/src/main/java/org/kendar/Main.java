package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.kendar.amqp.v09.plugins.AmqpRecordingPlugin;
import org.kendar.amqp.v09.plugins.AmqpReplayingPlugin;
import org.kendar.apis.ApiHandler;
import org.kendar.command.*;
import org.kendar.filters.PluginDescriptor;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.http.plugins.ErrorPlugin;
import org.kendar.http.plugins.HttpRecordingPlugin;
import org.kendar.http.plugins.HttpReplayingPlugin;
import org.kendar.mongo.plugins.MongoRecordingPlugin;
import org.kendar.mongo.plugins.MongoReplayingPlugin;
import org.kendar.mqtt.plugins.MqttRecordingPlugin;
import org.kendar.mqtt.plugins.MqttReplayingPlugin;
import org.kendar.mysql.plugins.MySqlRecordPlugin;
import org.kendar.mysql.plugins.MySqlReplayPlugin;
import org.kendar.postgres.plugins.PostgresRecordPlugin;
import org.kendar.postgres.plugins.PostgresReplayPlugin;
import org.kendar.redis.plugins.RedisRecordingPlugin;
import org.kendar.redis.plugins.RedisReplayingPlugin;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final ConcurrentHashMap<String, TcpServer> protocolServer = new ConcurrentHashMap<>();
    private static ProtocolsRunner om;
    private static HashMap<String, List<PluginDescriptor>> allFilters = new HashMap<>();

    public static void main(String[] args) throws Exception {
        execute(args, Main::stopWhenQuitCommand);
    }

    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) throws Exception {
        om = new ProtocolsRunner(
                new Amqp091Runner(),
                new MongoRunner(),
                new HttpRunner(),
                new JdbcRunner("mysql"),
                new JdbcRunner("postgres"),
                new MqttRunner(),
                new RedisRunner()
        );
        CommandLineParser parser = new DefaultParser();
        var options = ProtocolsRunner.getMainOptions();
        HashMap<String, List<PluginDescriptor>> filters = new HashMap<>();
        CommandLine cmd = parser.parse(options, args, true);
        var pluginsDir = cmd.getOptionValue("pluginsDir", "plugins");
        filters = loadFilters(pluginsDir);

        var ini = om.run(cmd, args, filters);
        execute(ini, stopWhenFalse, filters);
    }

    public static void stop() {
        for (var server : protocolServer.values()) {
            server.stop();
        }
    }

    private static Boolean stopWhenQuitCommand() {
        var scanner = new Scanner(System.in);
        System.out.println("Press Q to quit");
        String line = scanner.nextLine();
        try {
            if (line != null && line.trim().equalsIgnoreCase("q")) {
                System.out.println("Exiting");
                stop();
                return false;
            } else if (line != null) {
                System.out.println("Command not recognized: " + line.trim());
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            System.out.println("Exiting");
            System.err.println(ex);
            stop();
            return false;
        }
    }

    private static StorageRepository setupStorage(String logsDir) {

        if (logsDir != null && !logsDir.isEmpty()) {
            logsDir = logsDir.replace("{timestamp}", "" + new Date().getTime());
        }
        StorageRepository storage = new NullStorageRepository();
        if (logsDir != null && !logsDir.isEmpty()) {
            storage = new FileStorageRepository(Path.of(logsDir));
        }
        return storage;
    }

    private static HashMap<String, List<PluginDescriptor>> loadFilters(String pluginsDir) {
        if (!allFilters.isEmpty()) {
            return allFilters;
        }
        if (Path.of(pluginsDir).toAbsolutePath().toFile().exists()) {

            var pluginManager = new JarPluginManager(Path.of(pluginsDir).toAbsolutePath());
            pluginManager.loadPlugins();
            pluginManager.startPlugins();
            allFilters = new HashMap<String, List<PluginDescriptor>>();
            for (var item : pluginManager.getExtensions(PluginDescriptor.class)) {
                var protocol = item.getProtocol().toLowerCase();
                if (!allFilters.containsKey(protocol)) {
                    allFilters.put(protocol, new ArrayList<>());
                }
                allFilters.get(protocol).add(item);
            }
        }
        addEmbedded(allFilters, "http", List.of(
                new HttpRecordingPlugin(),
                new ErrorPlugin(),
                new HttpReplayingPlugin()));
        addEmbedded(allFilters, "mongodb", List.of(
                new MongoRecordingPlugin(),
                new MongoReplayingPlugin()));
        addEmbedded(allFilters, "redis", List.of(
                new RedisRecordingPlugin(),
                new RedisReplayingPlugin()));
        addEmbedded(allFilters, "amqp091", List.of(
                new AmqpRecordingPlugin(),
                new AmqpReplayingPlugin()));
        addEmbedded(allFilters, "mqtt", List.of(
                new MqttRecordingPlugin(),
                new MqttReplayingPlugin()));
        addEmbedded(allFilters, "postgres", List.of(
                new PostgresRecordPlugin(),
                new PostgresReplayPlugin()));
        addEmbedded(allFilters, "mysql", List.of(
                new MySqlRecordPlugin(),
                new MySqlReplayPlugin()));
        return allFilters;
    }

    private static void addEmbedded(HashMap<String, List<PluginDescriptor>> filters, String prt, List<ProtocolPluginDescriptor<?, ?>> embeddedFilters) {
        if (!filters.containsKey(prt)) {
            filters.put(prt, new ArrayList<>());
        }
        filters.get(prt).addAll(embeddedFilters);
    }


    public static boolean isRunning() {
        return protocolServer.values().stream().anyMatch(v -> v.isRunning());
    }


    public static void execute(GlobalSettings ini, Supplier<Boolean> stopWhenFalse, HashMap<String, List<PluginDescriptor>> allFilters) {
        if (ini == null) return;
        var logsDir = ProtocolsRunner.getOrDefault(ini.getDataDir(), "data");
        StorageRepository storage = setupStorage(logsDir);
        storage.initialize();
        ini.putService(storage.getType(), storage);

        var pluginsDir = ProtocolsRunner.getOrDefault(ini.getPluginsDir(), "plugins");
        if (allFilters == null || allFilters.isEmpty()) {
            allFilters = loadFilters(pluginsDir);
        }
        var logLevel = ProtocolsRunner.getOrDefault(ini.getLogLevel(), "ERROR");

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var logger = loggerContext.getLogger("org.kendar");
        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));

        var apiHandler = new ApiHandler();

        for (var item : ini.getProtocols().entrySet()) {
            try {
                var protocol = ini.getProtocolForKey(item.getKey());
                if (protocol == null) continue;
                var protocolManager = om.getManagerFor(protocol);
                var filters = loadAvailableFiltersForProtocol(protocol, ini, allFilters);
                var protocolFullSettings = ini.getProtocol(item.getKey(), protocolManager.getSettingsClass());

                try {
                    om.start(protocolServer, item.getKey(), ini, protocolFullSettings, storage, filters, stopWhenFalse);
                } catch (Exception e) {
                    protocolServer.remove(item);
                    throw new RuntimeException(e);
                }
                apiHandler.addProtocol(item.getKey(), protocolManager, filters, protocolFullSettings);


            } catch (Exception ex) {

            }
        }
        while (stopWhenFalse.get()) {
            Sleeper.sleep(100);
        }
    }

    private static List<PluginDescriptor> loadAvailableFiltersForProtocol(ProtocolSettings protocol, GlobalSettings global,
                                                                          HashMap<String, List<PluginDescriptor>> allFilters) {
        var availableFilters = allFilters.get(protocol.getProtocol());
        if (availableFilters == null) availableFilters = new ArrayList<>();
        var simplePlugins = protocol.getSimplePlugins();
        var filters = new ArrayList<PluginDescriptor>();

        for (var simplePlugin : simplePlugins.entrySet()) {
            var availableFilter = availableFilters.stream().filter(av -> av.getId().equalsIgnoreCase(simplePlugin.getValue().getPlugin())).findFirst();
            if (availableFilter.isPresent()) {
                var realFilter = availableFilter.get().clone();
                realFilter.setSettings(protocol.getPlugin(simplePlugin.getKey(), realFilter.getSettingClass()));
                filters.add(realFilter);

            }
        }
        return filters;
    }
}
