package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import org.kendar.apis.ApiFiltersLoader;
import org.kendar.apis.ApiHandler;
import org.kendar.apis.filters.FiltersConfiguration;
import org.kendar.cli.CommandParser;
import org.kendar.command.ProtocolsRunner;
import org.kendar.di.DiService;
import org.kendar.di.TpmScopeType;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.Sleeper;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.System.exit;

@SuppressWarnings("ThrowablePrintedToSystemOut")
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static ConcurrentHashMap<String, TcpServer> protocolServersCache;
    private static ProtocolsRunner protocolsRunner;
    private static JarPluginManager pluginManager;
    private static HttpServer apiServer;
    private static DiService diService;


    public static void main(String[] args) throws Exception {

        execute(args, Main::stopWhenQuitCommand);
        exit(0);
    }


    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) throws Exception {
        diService = new DiService();
        diService.loadPackage("org.kendar");

        protocolServersCache = new ConcurrentHashMap<>();
        var settings = new ChangeableReference<>(new GlobalSettings());

        var options = ProtocolsRunner.getMainOptions(settings);
        var parser = new CommandParser(options);
        parser.parseIgnoreMissing(args);
        diService.register(GlobalSettings.class, settings.get());

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
        for (var ec : pluginManager.getExtensionClasses(ProtocolPluginDescriptor.class)) {
            diService.bind(ec);
        }
        for (var ec : pluginManager.getExtensionClasses(GlobalPluginDescriptor.class)) {
            diService.bind(ec);
        }

        protocolsRunner = diService.getInstance(ProtocolsRunner.class);
        if (!parser.hasOption("cfg")) {
            if (!protocolsRunner.prepareSettingsFromCommandLine(options, args, settings.get(), parser)) {
                return;
            }
        }

        execute(settings.get(), stopWhenFalse);
    }

    public static void stop() {
        if (protocolServersCache == null) return;
        for (var server : protocolServersCache.values()) {
            server.stop();
        }
        if (apiServer != null) {
            apiServer.stop(0);
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

    public static boolean isRunning() {
        if (protocolServersCache == null) return false;
        return protocolServersCache.values().stream().anyMatch(TcpServer::isRunning);
    }


    public static void execute(GlobalSettings ini, Supplier<Boolean> stopWhenFalse) throws Exception {
        if (ini == null) return;
        diService.register(FileResourcesUtils.class, new FileResourcesUtils());
        var logLevel = ProtocolsRunner.getOrDefault(ini.getLogLevel(), "INFO");


        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var log = loggerContext.getLogger("org.kendar");
        log.setLevel(Level.toLevel(logLevel, Level.ERROR));
        diService.register(FiltersConfiguration.class, new FiltersConfiguration());
        var apisFiltersLoader = diService.getInstance(ApiFiltersLoader.class);

        var started = new AtomicInteger(0);
        for (var item : ini.getProtocols().entrySet()) {
            new Thread(() -> {
                try {
                    var localDiService = diService.createChildScope(TpmScopeType.THREAD);
                    var protocol = ini.getProtocolForKey(item.getKey());
                    if (protocol == null) return;
                    var protocolManager = protocolsRunner.getManagerFor(protocol);
                    var availableProtocolPlugins = localDiService.getInstances(ProtocolPluginDescriptor.class, protocol.getProtocol());
                    var protocolFullSettings = ini.getProtocol(item.getKey(), protocolManager.getSettingsClass());

                    var storage = localDiService.getInstance(StorageRepository.class);
                    try {
                        protocolsRunner.start(protocolServersCache,
                                item.getKey(), ini, protocolFullSettings, storage, availableProtocolPlugins,
                                stopWhenFalse);
                    } catch (Exception e) {
                        //noinspection SuspiciousMethodCalls
                        protocolServersCache.remove(item);
                        throw new RuntimeException(e);
                    }
                    var pi = new ProtocolInstance(item.getKey(),
                            protocolServersCache.get(item.getKey()), availableProtocolPlugins, protocolFullSettings);
                    var apiHandler = localDiService.getInstance(ApiHandler.class);
                    apiHandler.addProtocol(pi);
                    for (var pl : pi.getPlugins()) {
                        var apiHandlerPlugin = pl.getApiHandler();
                        apisFiltersLoader.getFilters().addAll(apiHandlerPlugin);
                    }
                    started.incrementAndGet();
                    ini.putService(item.getKey(), pi);

                } catch (Exception ex) {
                    log.error("Unable to start protocol {}", item.getKey(), ex);
                }
            }).start();
        }
        var globalPlugins = diService.getInstances(GlobalPluginDescriptor.class);
        for (int i = globalPlugins.size() - 1; i >= 0; i--) {
            var plugin = globalPlugins.get(i);
            var pluginSettings = (PluginSettings) ini.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (pluginSettings != null) {
                plugin.initialize(ini, pluginSettings);
            } else {
                globalPlugins.remove(i);
            }
        }
        new Thread(() -> {
            try {
                while (started.get() < ini.getProtocols().size()) {
                    Sleeper.sleep(100);
                }
                for (var item : protocolServersCache.values()) {
                    var protocolFilter = item.getProtoDescriptor().getApiHandler();
                    if (protocolFilter != null) {
                        apisFiltersLoader.getFilters().addAll(protocolFilter);
                    }
                }
                apisFiltersLoader.loadFilters();
                if (ini.getApiPort() > 0) {
                    var address = new InetSocketAddress(ini.getApiPort());
                    apiServer = HttpServer.create(address, 10);
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
}
