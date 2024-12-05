package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import org.kendar.amqp.v09.plugins.AmqpRecordPlugin;
import org.kendar.amqp.v09.plugins.AmqpReplayPlugin;
import org.kendar.amqp.v09.plugins.AmqpReportPlugin;
import org.kendar.apis.ApiFiltersLoader;
import org.kendar.apis.ApiHandler;
import org.kendar.cli.CommandParser;
import org.kendar.command.*;
import org.kendar.http.plugins.*;
import org.kendar.mongo.plugins.MongoRecordPlugin;
import org.kendar.mongo.plugins.MongoReplayPlugin;
import org.kendar.mongo.plugins.MongoReportPlugin;
import org.kendar.mqtt.plugins.MqttRecordPlugin;
import org.kendar.mqtt.plugins.MqttReplayPlugin;
import org.kendar.mqtt.plugins.MqttReportPlugin;
import org.kendar.mysql.plugins.*;
import org.kendar.plugins.GlobalReportPlugin;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.postgres.plugins.*;
import org.kendar.redis.plugins.RedisRecordPlugin;
import org.kendar.redis.plugins.RedisReplayPlugin;
import org.kendar.redis.plugins.RedisReportPlugin;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.Sleeper;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("ThrowablePrintedToSystemOut")
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static ConcurrentHashMap<String, TcpServer> protocolServersCache;
    private static ProtocolsRunner protocolsRunner;
    private static HashMap<String, List<ProtocolPluginDescriptor>> allProtocolSpecificPlugins;
    private static List<GlobalPluginDescriptor> allGlobalPlugins;
    private static List<GlobalPluginDescriptor> globalPlugins;
    private static JarPluginManager pluginManager;


    public static void main(String[] args) throws Exception {
        execute(args, Main::stopWhenQuitCommand);
    }


    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) throws Exception {
        protocolServersCache = new ConcurrentHashMap<>();
        allProtocolSpecificPlugins = new HashMap<>();
        allGlobalPlugins = new ArrayList<>();
        globalPlugins = new ArrayList<>();
        protocolsRunner = new ProtocolsRunner(
                new Amqp091Runner(),
                new MongoRunner(),
                new HttpRunner(),
                new JdbcRunner("mysql"),
                new JdbcRunner("postgres"),
                new MqttRunner(),
                new RedisRunner()
        );
        var settings = new ChangeableReference<>(new GlobalSettings());

        var options = ProtocolsRunner.getMainOptions(settings);
        var parser = new CommandParser(options);
        parser.parseIgnoreMissing(args);

        if (parser.hasOption("unattended") || settings.get().isUnattended()) {
            stopWhenFalse = () -> {
                Sleeper.sleep(10000);
                return true;
            };
        }
        var pluginsDir = settings.get().getPluginsDir();
        var pathOfPluginsDir = Path.of(pluginsDir).toAbsolutePath();
        if (!pathOfPluginsDir.toFile().exists()) {
            Files.createDirectories(pathOfPluginsDir);
        }

        pluginManager = new JarPluginManager(pathOfPluginsDir);
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        var protocolPlugins = loadProtocolPlugins(pluginsDir);
        globalPlugins = loadGlobalPlugins(pluginsDir);
        if (!parser.hasOption("cfg")) {
            if (!protocolsRunner.prepareSettingsFromCommandLine(options, args, protocolPlugins, settings.get(), parser)) {
                return;
            }
        }

        execute(settings.get(), stopWhenFalse, protocolPlugins, globalPlugins);
    }

    public static void stop() {
        if(protocolServersCache==null)return;
        for (var server : protocolServersCache.values()) {
            server.stop();
        }
    }

    private static Boolean stopWhenQuitCommand() {
        var scanner = new Scanner(System.in);
        System.out.println("Press Q to quit");
        String line;
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


    private static HashMap<String, List<ProtocolPluginDescriptor>> loadProtocolPlugins(String pluginsDir) {
        if (!allProtocolSpecificPlugins.isEmpty()) {
            return allProtocolSpecificPlugins;
        }
        var pathOfPluginsDir = Path.of(pluginsDir).toAbsolutePath();
        if (pathOfPluginsDir.toFile().exists()) {


            allProtocolSpecificPlugins = new HashMap<>();
            for (var item : pluginManager.getExtensions(ProtocolPluginDescriptor.class)) {
                var protocol = item.getProtocol().toLowerCase();
                if (!allProtocolSpecificPlugins.containsKey(protocol)) {
                    allProtocolSpecificPlugins.put(protocol, new ArrayList<>());
                }
                allProtocolSpecificPlugins.get(protocol).add(item);
            }
        }
        var ssl = new SSLDummyPlugin();
        ssl.setActive(true);
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "http", List.of(
                new HttpRecordPlugin(),
                new HttpErrorPlugin(),
                new HttpReplayPlugin(),
                new HttpReportPlugin(),
                new HttpRewritePlugin(),
                new HttpMockPlugin(),
                new HttpLatencyPlugin(),
                new HttpRateLimitPlugin(), ssl
        ));
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "mongodb", List.of(
                new MongoRecordPlugin(),
                new MongoReplayPlugin(),
                new MongoReportPlugin()));
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "redis", List.of(
                new RedisRecordPlugin(),
                new RedisReplayPlugin(),
                new RedisReportPlugin()));
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "amqp091", List.of(
                new AmqpRecordPlugin(),
                new AmqpReplayPlugin(),
                new AmqpReportPlugin()));
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "mqtt", List.of(
                new MqttRecordPlugin(),
                new MqttReplayPlugin(),
                new MqttReportPlugin()));
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "postgres", List.of(
                new PostgresRecordPlugin(),
                new PostgresReplayPlugin(),
                new PostgresRewritePlugin(),
                new PostgresReportPlugin(),
                new PostgresMockPlugin()));
        addEmbeddedProtocolPlugin(allProtocolSpecificPlugins, "mysql", List.of(
                new MySqlRecordPlugin(),
                new MySqlReplayPlugin(),
                new MySqlRewritePlugin(),
                new MySqlReportPlugin(),
                new MySqlMockPlugin()));
        return allProtocolSpecificPlugins;
    }

    private static void addEmbeddedProtocolPlugin(HashMap<String, List<ProtocolPluginDescriptor>> plugins, String prt, List<ProtocolPluginDescriptor<?>> embeddedPlugins) {
        if (!plugins.containsKey(prt)) {
            plugins.put(prt, new ArrayList<>());
        }
        plugins.get(prt).addAll(embeddedPlugins);
    }


    public static boolean isRunning() {
        if(protocolServersCache==null)return false;
        return protocolServersCache.values().stream().anyMatch(TcpServer::isRunning);
    }


    public static void execute(GlobalSettings ini, Supplier<Boolean> stopWhenFalse,
                               HashMap<String, List<ProtocolPluginDescriptor>> protocolPlugins,
                               List<GlobalPluginDescriptor> globalPlugins) throws Exception {
        if (ini == null) return;
        var logsDir = ProtocolsRunner.getOrDefault(
                ini.getDataDir(),
                Path.of("data",
                        Long.toString(Calendar.getInstance().getTimeInMillis())).toAbsolutePath().toString());
        StorageRepository storage = setupStorage(logsDir);
        storage.initialize();
        ini.putService(storage.getType(), storage);

        var pluginsDir = ProtocolsRunner.getOrDefault(ini.getPluginsDir(), "plugins");


        if (protocolPlugins == null || protocolPlugins.isEmpty()) {
            protocolPlugins = loadProtocolPlugins(pluginsDir);
        }
        if (globalPlugins == null || globalPlugins.isEmpty()) {
            globalPlugins = loadGlobalPlugins(pluginsDir);
        }
        globalPlugins.add(new GlobalReportPlugin());


        var logLevel = ProtocolsRunner.getOrDefault(ini.getLogLevel(), "INFO");


        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var logger = loggerContext.getLogger("org.kendar");
        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));
        var apisFiltersLoader = new ApiFiltersLoader(new ArrayList<>(), ini.getApiPort());
        var apiHandler = new ApiHandler(ini);
        apisFiltersLoader.getFilters().add(apiHandler);
        apiHandler.addGLobalPlugins(globalPlugins);

        for (var gp : globalPlugins) {
            var ah = gp.getApiHandler();
            apisFiltersLoader.getFilters().add(ah);
        }

        var finalAllPlugins = protocolPlugins;
        var started = new AtomicInteger(0);
        for (var item : ini.getProtocols().entrySet()) {
            new Thread(() -> {
                try {

                    var protocol = ini.getProtocolForKey(item.getKey());
                    if (protocol == null) return;
                    var protocolManager = protocolsRunner.getManagerFor(protocol);
                    var availableProtocolPlugins = loadAvailablePluginsForProtocol(protocol, ini, finalAllPlugins);
                    var protocolFullSettings = ini.getProtocol(item.getKey(), protocolManager.getSettingsClass());

                    try {
                        protocolsRunner.start(protocolServersCache, item.getKey(), ini, protocolFullSettings, storage, availableProtocolPlugins, stopWhenFalse);
                    } catch (Exception e) {
                        //noinspection SuspiciousMethodCalls
                        protocolServersCache.remove(item);
                        throw new RuntimeException(e);
                    }
                    var pi = new ProtocolInstance(item.getKey(),
                            protocolServersCache.get(item.getKey()), availableProtocolPlugins, protocolFullSettings);
                    apiHandler.addProtocol(pi);
                    for (var pl : pi.getPlugins()) {
                        var apiHandlerPlugin = pl.getApiHandler();
                        apisFiltersLoader.getFilters().add(apiHandlerPlugin);
                    }
                    started.incrementAndGet();
                    ini.putService(item.getKey(), pi);

                } catch (Exception ex) {
                    log.error("Unable to start protocol {}", item.getKey(), ex);
                }
            }).start();
        }
        for (int i = globalPlugins.size() - 1; i >= 0; i--) {
            var plugin = globalPlugins.get(i);
            var pluginSettings = (PluginSettings) ini.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (pluginSettings != null) {
                plugin.initialize(ini, pluginSettings);
            }else{
                globalPlugins.remove(i);
            }
        }
        new Thread(() -> {
            try {
                while (started.get() < ini.getProtocols().size()) {
                    Sleeper.sleep(100);
                }
                apisFiltersLoader.loadFilters();
                if (ini.getApiPort() > 0) {
                    var address = new InetSocketAddress(ini.getApiPort());
                    var apiServer = HttpServer.create(address, 10);
                    apiServer.createContext("/", apisFiltersLoader);
                    apiServer.start();
                    log.info("[SERVER][IN] Listening on *.:{} TPM Apis", ini.getApiPort());
                }
            } catch (Exception e) {
                log.error("Unable to start API serer", e);
            }
        }).start();

        while (stopWhenFalse.get()) {
            Sleeper.sleep(100);
        }
    }

    private static List<ProtocolPluginDescriptor> loadAvailablePluginsForProtocol(ProtocolSettings protocol, GlobalSettings global,
                                                                                  HashMap<String, List<ProtocolPluginDescriptor>> allPlugins) {
        var availablePlugins = allPlugins.get(protocol.getProtocol());
        if (availablePlugins == null) availablePlugins = new ArrayList<>();
        var simplePlugins = protocol.getSimplePlugins();
        var plugins = new ArrayList<ProtocolPluginDescriptor>();

        for (var simplePlugin : simplePlugins.entrySet()) {
            var availablePlugin = availablePlugins.stream().filter(av -> av.getId().equalsIgnoreCase(simplePlugin.getKey())).findFirst();
            if (availablePlugin.isPresent()) {
                var pluginInstance = availablePlugin.get().duplicate();
                plugins.add((ProtocolPluginDescriptor) pluginInstance);
            }
        }
        var alwaysActives = availablePlugins.stream().filter(av -> AlwaysActivePlugin.class.isAssignableFrom(av.getClass())).collect(Collectors.toList());
        for (var alwaysActive : alwaysActives) {
            var pluginInstance = alwaysActive.duplicate();
            plugins.add((ProtocolPluginDescriptor) pluginInstance);
        }
        return plugins;
    }


    private static List<GlobalPluginDescriptor> loadGlobalPlugins(String pluginsDir) {
        if (!allGlobalPlugins.isEmpty()) {
            return allGlobalPlugins;
        }
        allGlobalPlugins = new ArrayList<>();
        allGlobalPlugins.addAll(pluginManager.getExtensions(GlobalPluginDescriptor.class));

        return allGlobalPlugins;
    }
}
