package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.kendar.command.*;
import org.kendar.filters.FilterDescriptor;
import org.kendar.server.TcpServer;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(OldMain.class);
    private static final ConcurrentHashMap<String, TcpServer> protocolServer = new ConcurrentHashMap<>();
    private static OptionsManager om;

    public static void main(String[] args) {
        execute(args, Main::stopWhenQuitCommand);
    }

    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) {
        om = new OptionsManager(
                new Amqp091Protocol(),
                new MongoProtocol(),
                new HttpProtocol(),
                new JdbcProtocol("mysql"),
                new JdbcProtocol("postgres"),
                new MqttProtocol(),
                new RedisProtocol()
        );
        var ini = om.run(args);
        execute(ini, stopWhenFalse);
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

    private static HashMap<String, List<FilterDescriptor>> loadFilters(String pluginsDir) {
        if (!Path.of(pluginsDir).toAbsolutePath().toFile().exists()) {
            return new HashMap<>();
        }
        var pluginManager = new JarPluginManager(Path.of(pluginsDir).toAbsolutePath());
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        var filters = new HashMap<String, List<FilterDescriptor>>();
        for (var item : pluginManager.getExtensions(FilterDescriptor.class)) {
            var protocol = item.getProtocol().toLowerCase();
            if (!filters.containsKey(protocol)) {
                filters.put(protocol, new ArrayList<>());
            }
            filters.get(protocol).add(item);
        }
        return filters;
    }


    private static ArrayList<FilterDescriptor> loadCorrectFiltersForProtocol(String id, String protocol, HashMap<String, List<FilterDescriptor>> allFilters, Ini ini) {
        var availableFilters = allFilters.get(protocol.toLowerCase());
        if (availableFilters == null) availableFilters = new ArrayList<>();
        var filters = new ArrayList<FilterDescriptor>();
        if (ini != null) {

            for (var availableFilter : availableFilters) {
                var sectionId = id + "-" + availableFilter.getId();
                var section = ini.getSection(sectionId);
                if (section != null && !section.isEmpty()) {
                    var clonedFilter = availableFilter.clone();
                    filters.add(clonedFilter);
                }
            }
        }
        return filters;
    }


    public static boolean isRunning() {
        return protocolServer.values().stream().anyMatch(v -> v.isRunning());
    }


    public static void execute(Ini ini, Supplier<Boolean> stopWhenFalse) {
        if (ini == null) return;
        var logsDir = ini.getValue("global", "datadir", String.class);
        StorageRepository storage = setupStorage(logsDir);

        var pluginsDir = ini.getValue("global", "pluginsDir", String.class, "plugins");
        var allFilters = loadFilters(pluginsDir);
        var logLevel = ini.getValue("global", "loglevel", String.class, "ERROR");

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var logger = loggerContext.getLogger("org.kendar");
        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));

        for (var key : ini.getSections()) {
            try {
                var protocol = ini.getValue(key, "protocol", String.class);
                if (protocol == null || protocol.isEmpty()) continue;
                var filters = loadCorrectFiltersForProtocol(key, protocol, allFilters, ini);
                new Thread(() -> {
                    try {
                        om.start(protocolServer, key, ini, protocol, storage, filters, stopWhenFalse);
                    } catch (Exception e) {
                        protocolServer.remove(key);
                        throw new RuntimeException(e);
                    }
                }).start();

            } catch (Exception ex) {

            }
        }
        while (stopWhenFalse.get()) {
            Sleeper.sleep(100);
        }
    }
}
