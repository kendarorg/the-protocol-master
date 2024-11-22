package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.kendar.amqp.v09.plugins.AmqpRecordingPlugin;
import org.kendar.amqp.v09.plugins.AmqpReplayingPlugin;
import org.kendar.apis.ApiHandler;
import org.kendar.apis.ApiServerHandler;
import org.kendar.command.*;
import org.kendar.http.plugins.*;
import org.kendar.mongo.plugins.MongoRecordingPlugin;
import org.kendar.mongo.plugins.MongoReplayingPlugin;
import org.kendar.mqtt.plugins.MqttRecordingPlugin;
import org.kendar.mqtt.plugins.MqttReplayingPlugin;
import org.kendar.mysql.plugins.MySqlRecordPlugin;
import org.kendar.mysql.plugins.MySqlReplayPlugin;
import org.kendar.mysql.plugins.MySqlRewritePlugin;
import org.kendar.plugins.AlwaysActivePlugin;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.postgres.plugins.PostgresRecordPlugin;
import org.kendar.postgres.plugins.PostgresReplayPlugin;
import org.kendar.postgres.plugins.PostgresRewritePlugin;
import org.kendar.redis.plugins.RedisRecordingPlugin;
import org.kendar.redis.plugins.RedisReplayingPlugin;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("ThrowablePrintedToSystemOut")
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final ConcurrentHashMap<String, TcpServer> protocolServer = new ConcurrentHashMap<>();
    private static ProtocolsRunner om;
    private static HashMap<String, List<PluginDescriptor>> allPlugins = new HashMap<>();

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
        HashMap<String, List<PluginDescriptor>> plugins = new HashMap<>();
        CommandLine cmd = parser.parse(options, args, true);
        if (cmd.hasOption("unattended")) {
            stopWhenFalse = () -> {
                Sleeper.sleep(10000);
                return true;
            };
        }
        var pluginsDir = cmd.getOptionValue("pluginsDir", "plugins");
        plugins = loadPlugins(pluginsDir);

        var ini = om.run(cmd, args, plugins);
        execute(ini, stopWhenFalse, plugins);
    }

    public static void stop() {
        for (var server : protocolServer.values()) {
            server.stop();
        }
    }

    private static Boolean stopWhenQuitCommand() {
        var scanner = new Scanner(System.in);
        System.out.println("Press Q to quit");
        String line = "";
        line = scanner.nextLine();
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

    private static HashMap<String, List<PluginDescriptor>> loadPlugins(String pluginsDir) {
        if (!allPlugins.isEmpty()) {
            return allPlugins;
        }
        var pathOfPluginsDir = Path.of(pluginsDir).toAbsolutePath();
        if (pathOfPluginsDir.toFile().exists()) {

            var pluginManager = new JarPluginManager(pathOfPluginsDir);
            pluginManager.loadPlugins();
            pluginManager.startPlugins();
            allPlugins = new HashMap<>();
            for (var item : pluginManager.getExtensions(PluginDescriptor.class)) {
                var protocol = item.getProtocol().toLowerCase();
                if (!allPlugins.containsKey(protocol)) {
                    allPlugins.put(protocol, new ArrayList<>());
                }
                allPlugins.get(protocol).add(item);
            }
        }
        var ssl = new SSLDummyPlugin();
        ssl.setActive(true);
        addEmbedded(allPlugins, "http", List.of(
                new HttpRecordingPlugin(),
                new HttpErrorPlugin(),
                new HttpReplayingPlugin(),
                new HttpRewritePlugin(),
                new HttpMockPlugin(),
                new HttpLatencyPlugin(), ssl
        ));
        addEmbedded(allPlugins, "mongodb", List.of(
                new MongoRecordingPlugin(),
                new MongoReplayingPlugin()));
        addEmbedded(allPlugins, "redis", List.of(
                new RedisRecordingPlugin(),
                new RedisReplayingPlugin()));
        addEmbedded(allPlugins, "amqp091", List.of(
                new AmqpRecordingPlugin(),
                new AmqpReplayingPlugin()));
        addEmbedded(allPlugins, "mqtt", List.of(
                new MqttRecordingPlugin(),
                new MqttReplayingPlugin()));
        addEmbedded(allPlugins, "postgres", List.of(
                new PostgresRecordPlugin(),
                new PostgresReplayPlugin(),
                new PostgresRewritePlugin()));
        addEmbedded(allPlugins, "mysql", List.of(
                new MySqlRecordPlugin(),
                new MySqlReplayPlugin(),
                new MySqlRewritePlugin()));
        return allPlugins;
    }

    private static void addEmbedded(HashMap<String, List<PluginDescriptor>> plugins, String prt, List<ProtocolPluginDescriptor<?, ?>> embeddedPlugins) {
        if (!plugins.containsKey(prt)) {
            plugins.put(prt, new ArrayList<>());
        }
        plugins.get(prt).addAll(embeddedPlugins);
    }


    public static boolean isRunning() {
        return protocolServer.values().stream().anyMatch(TcpServer::isRunning);
    }


    public static void execute(GlobalSettings ini, Supplier<Boolean> stopWhenFalse, HashMap<String, List<PluginDescriptor>> allPlugins) throws Exception {
        if (ini == null) return;
        var logsDir = ProtocolsRunner.getOrDefault(
                ini.getDataDir(),
                Path.of("data",
                        Long.toString(Calendar.getInstance().getTimeInMillis())).toAbsolutePath().toString());
        StorageRepository storage = setupStorage(logsDir);
        storage.initialize();
        ini.putService(storage.getType(), storage);

        var pluginsDir = ProtocolsRunner.getOrDefault(ini.getPluginsDir(), "plugins");
        if (allPlugins == null || allPlugins.isEmpty()) {
            allPlugins = loadPlugins(pluginsDir);
        }
        var logLevel = ProtocolsRunner.getOrDefault(ini.getLogLevel(), "ERROR");


        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var logger = loggerContext.getLogger("org.kendar");
        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));

        var apiHandler = new ApiHandler(ini);

        var finalAllPlugins = allPlugins;
        for (var item : ini.getProtocols().entrySet()) {
            new Thread(() -> {
                try {

                    var protocol = ini.getProtocolForKey(item.getKey());
                    if (protocol == null) return;
                    var protocolManager = om.getManagerFor(protocol);
                    var availablePlugins = loadAvailablePluginsForProtocol(protocol, ini, finalAllPlugins);
                    var protocolFullSettings = ini.getProtocol(item.getKey(), protocolManager.getSettingsClass());

                    try {
                        om.start(protocolServer, item.getKey(), ini, protocolFullSettings, storage, availablePlugins, stopWhenFalse);
                    } catch (Exception e) {
                        //noinspection SuspiciousMethodCalls
                        protocolServer.remove(item);
                        throw new RuntimeException(e);
                    }
                    apiHandler.addProtocol(item.getKey(), protocolManager, availablePlugins, protocolFullSettings);


                } catch (Exception ex) {

                }
            }).start();
        }
        if (ini.getApiPort() > 0) {
            var address = new InetSocketAddress(ini.getApiPort());
            var apiServer = HttpServer.create(address, 10);
            apiServer.createContext("/", new ApiServerHandler(apiHandler));
            apiServer.start();
            log.info("[CL>TP][IN] Listening on *.:{} TPM Apis", ini.getApiPort());
        }
        while (stopWhenFalse.get()) {
            Sleeper.sleep(100);
        }
    }

    private static List<PluginDescriptor> loadAvailablePluginsForProtocol(ProtocolSettings protocol, GlobalSettings global,
                                                                          HashMap<String, List<PluginDescriptor>> allPlugins) {
        var availablePlugins = allPlugins.get(protocol.getProtocol());
        if (availablePlugins == null) availablePlugins = new ArrayList<>();
        var simplePlugins = protocol.getSimplePlugins();
        var plugins = new ArrayList<PluginDescriptor>();

        for (var simplePlugin : simplePlugins.entrySet()) {
            var availablePlugin = availablePlugins.stream().filter(av -> av.getId().equalsIgnoreCase(simplePlugin.getKey())).findFirst();
            if (availablePlugin.isPresent()) {
                var pluginInstance = availablePlugin.get().clone();
                pluginInstance.setSettings(protocol.getPlugin(simplePlugin.getKey(), pluginInstance.getSettingClass()));
                plugins.add(pluginInstance);
            }
        }
        var alwaysActives = availablePlugins.stream().filter(av -> AlwaysActivePlugin.class.isAssignableFrom(av.getClass())).collect(Collectors.toList());
        for (var alwaysActive : alwaysActives) {
            var pluginInstance = alwaysActive.clone();
            pluginInstance.setSettings(new PluginSettings());
            plugins.add(pluginInstance);
        }
        return plugins;
    }
}
