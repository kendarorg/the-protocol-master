package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.kendar.apis.ApiHandler;
import org.kendar.command.*;
import org.kendar.filters.PluginDescriptor;
import org.kendar.http.plugins.ErrorPlugin;
import org.kendar.http.plugins.RecordingPlugin;
import org.kendar.http.plugins.ReplayPlugin;
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

    public static void main(String[] args)throws Exception {
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
        if (!Path.of(pluginsDir).toAbsolutePath().toFile().exists()) {
            return new HashMap<>();
        }
        var pluginManager = new JarPluginManager(Path.of(pluginsDir).toAbsolutePath());
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        var filters = new HashMap<String, List<PluginDescriptor>>();
        for (var item : pluginManager.getExtensions(PluginDescriptor.class)) {
            var protocol = item.getProtocol().toLowerCase();
            if (!filters.containsKey(protocol)) {
                filters.put(protocol, new ArrayList<>());
            }
            filters.get(protocol).add(item);
        }
        if(!filters.containsKey("http")){
            filters.put("http", new ArrayList<>());
        }
        filters.get("http").addAll(List.of(
                new RecordingPlugin(),
                new ErrorPlugin(),new ReplayPlugin()));
        return filters;
    }


    public static boolean isRunning() {
        return protocolServer.values().stream().anyMatch(v -> v.isRunning());
    }


    public static void execute(GlobalSettings ini, Supplier<Boolean> stopWhenFalse, HashMap<String, List<PluginDescriptor>> allFilters) {
        if (ini == null) return;
        var logsDir = ProtocolsRunner.getOrDefault(ini.getDataDir(), "data");
        StorageRepository storage = setupStorage(logsDir);

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
                var protocolFullSettings = ini.getProtocol(item.getKey(),protocolManager.getSettingsClass());

                try {
                    om.start(protocolServer, item.getKey(), ini, protocolFullSettings, storage, filters, stopWhenFalse);
                } catch (Exception e) {
                    protocolServer.remove(item);
                    throw new RuntimeException(e);
                }
                apiHandler.addProtocol(item.getKey(),protocolManager,filters,protocolFullSettings);


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

        for(var simplePlugin:simplePlugins.entrySet()){
            var availableFilter = availableFilters.stream().filter(av->av.getId().equalsIgnoreCase(simplePlugin.getValue().getPlugin())).findFirst();
            if(availableFilter.isPresent()){
                var realFilter = availableFilter.get().clone();
                realFilter.setSettings(protocol.getPlugin(simplePlugin.getKey(),realFilter.getSettingClass()));
                filters.add(realFilter);

            }
        }
        return filters;
    }
}
