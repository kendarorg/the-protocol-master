package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import org.kendar.apis.ApiFiltersLoader;
import org.kendar.apis.ApiHandler;
import org.kendar.apis.filters.FiltersConfiguration;
import org.kendar.cli.CommandOptions;
import org.kendar.cli.CommandParser;
import org.kendar.command.GlobalPluginCommandLineHandler;
import org.kendar.command.PluginCommandLineHandler;
import org.kendar.command.ProtocolCommandLineHandler;
import org.kendar.command.ProtocolsRunner;
import org.kendar.di.DiService;
import org.kendar.di.TpmScopeType;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.PluginsLoggerFactory;
import org.kendar.utils.Sleeper;
import org.pf4j.ExtensionPoint;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static java.lang.System.exit;

@SuppressWarnings("ThrowablePrintedToSystemOut")
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static ConcurrentHashMap<String, TcpServer> protocolServersCache;
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

        var options = ProtocolsRunner.getMainOptions(settings, diService);
        var parser = new CommandParser(options);
        parser.parseIgnoreMissing(args);
        diService.register(GlobalSettings.class, settings.get());
        diService.register(DiService.class, diService);

        if (parser.hasOption("unattended") || settings.get().isUnattended()) {
            stopWhenFalse = () -> {
                Sleeper.sleep(500);
                return true;
            };
        }
        //var localStopWhenFalse = stopWhenFalse;
        var pluginsDir = settings.get().getPluginsDir();
        var pathOfPluginsDir = Path.of(pluginsDir).toAbsolutePath();
        if (!pathOfPluginsDir.toFile().exists()) {
            Files.createDirectories(pathOfPluginsDir);
        }

        pluginManager = new JarPluginManager(pathOfPluginsDir);

        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        for (var plugin : pluginManager.getPlugins()) {
            for (var ec : pluginManager.getExtensionClasses(ExtensionPoint.class, plugin.getPluginId())) {
                diService.bind(ec);
            }
        }

        if (!parser.hasOption("cfg")) {
            var protocolMotherOption = options.getCommandOption("p");
            var protocolOptionsToAdd = new ArrayList<CommandOptions>();
            String helpForProtocol = null;
            if (parser.hasOption("help")) {
                helpForProtocol = parser.getOptionValue("help");
            }

            for (var globalPluginOptions : diService.getInstances(GlobalPluginCommandLineHandler.class)) {
                globalPluginOptions.setup(options, settings.get());
            }
            var commandLineOptions = diService.getInstances(ProtocolCommandLineHandler.class);
            for (var commandLineOption : commandLineOptions) {
                var tags = DiService.getTags(commandLineOption);
                if (helpForProtocol == null || tags.contains(helpForProtocol)) {
                    var protocolFilterCmdOptions = new ArrayList<PluginCommandLineHandler>();
                    for (var tag : tags) {
                        protocolFilterCmdOptions.addAll(diService.getInstances(PluginCommandLineHandler.class, tag));
                    }
                    commandLineOption.initializeFiltersCommandLineHandlers(protocolFilterCmdOptions);
                    protocolOptionsToAdd.add(commandLineOption.loadCommandLine(settings.get()));
                }
            }
            protocolMotherOption.withSubChoices(protocolOptionsToAdd.toArray(new CommandOptions[0]));
            if (parser.hasOption("help")) {
                parser.printHelp();
                return;
            }
            try {
                parser.parse(args);
            } catch (Exception e) {
                parser.printHelp();
                return;
            }
        }
        execute(settings.get(), stopWhenFalse);
//ZIPSETTINGS         var shouldRun = true;
//        var storageReloaded = new ChangeableReference<StorageReloadedEvent>(null);
//        Supplier<Boolean> newStopWhenFalse = ()->{
//            if(storageReloaded.get()!=null && Files.exists(Path.of(storageReloaded.get().getSettings()))) return false;
//            return localStopWhenFalse.get();
//        };
//        EventsQueue.register("main", value -> {
//            storageReloaded.set(value);
//
//        }, StorageReloadedEvent.class);
//        while(shouldRun) {
//
//            execute(settings.get(), newStopWhenFalse);
//            if (storageReloaded.get() != null && Files.exists(Path.of(storageReloaded.get().getSettings()))) {
//                stop();
//                Sleeper.sleep(1000);
//                ProtocolsRunner.loadConfigFile(settings,Path.of(storageReloaded.get().getSettings()).toString());
//                storageReloaded.set(null);
//            }else{
//                shouldRun = false;
//            }
//        }
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

    public static boolean isRunning() {
        if (protocolServersCache == null) return false;
        return protocolServersCache.values().stream().anyMatch(TcpServer::isRunning);
    }


    public static void execute(GlobalSettings ini, Supplier<Boolean> stopWhenFalse) throws Exception {
        if (ini == null) return;

        var logLevel = ProtocolsRunner.getOrDefault(ini.getLogLevel(), "INFO");


        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var log = loggerContext.getLogger("org.kendar");
        log.setLevel(Level.toLevel(logLevel, Level.ERROR));
        diService.register(PluginsLoggerFactory.class, new PluginsLoggerFactory());

        if (!ini.getDataDir().contains("=")) {
            ini.setDataDir("file=" + ini.getDataDir());
        }
        var dataDir = ini.getDataDir();
        var splitted = dataDir.split("=", 2);
        var storage_type = "storage_" + splitted[0];
        var storageInstance = diService.getInstance(StorageRepository.class, storage_type);
        diService.overwrite(StorageRepository.class, storageInstance);

        diService.register(FileResourcesUtils.class, new FileResourcesUtils());
        diService.register(FiltersConfiguration.class, new FiltersConfiguration());
        var apisFiltersLoader = diService.getInstance(ApiFiltersLoader.class);

        CountDownLatch latch = new CountDownLatch(ini.getProtocols().size());
        for (var item : ini.getProtocols().entrySet()) {
            new Thread(() -> {
                try {
                    var localDiService = diService.createChildScope(TpmScopeType.THREAD);
                    try {
                        var protocol = ini.getProtocolForKey(item.getKey());
                        if (protocol == null) return;
                        //Retrieve the type
                        var tempSettings = localDiService.getInstance(ProtocolSettings.class, protocol.getProtocol());
                        //Load the real data
                        var protocolFullSettings = ini.getProtocol(item.getKey(), tempSettings.getClass());
                        protocolFullSettings.setProtocolInstanceId(item.getKey());
                        //Overwrite
                        localDiService.overwrite(ProtocolSettings.class, protocolFullSettings);
                        localDiService.overwrite(protocolFullSettings.getClass(), protocolFullSettings);

                        var baseProtocol = localDiService.getInstance(NetworkProtoDescriptor.class, protocol.getProtocol());
                        baseProtocol.initialize();
                        var ps = new TcpServer(baseProtocol);
                        ps.setOnStart(() -> DiService.setThreadContext(localDiService));
                        ps.start();
                        Sleeper.sleep(5000, ps::isRunning);
                        protocolServersCache.put(item.getKey(), ps);

                        var pi = new ProtocolInstance(item.getKey(),
                                protocolServersCache.get(item.getKey()),
                                baseProtocol.getPlugins().stream().map(a -> (ProtocolPluginDescriptor) a).toList(),
                                protocolFullSettings);
                        var apiHandler = localDiService.getInstance(ApiHandler.class);
                        apiHandler.addProtocol(pi);
                        for (var pl : protocolServersCache.get(item.getKey()).getProtoDescriptor().getPlugins()) {
                            var apiHandlerPlugin = ((ProtocolPluginDescriptor) pl).getApiHandler();
                            apisFiltersLoader.getFilters().addAll(apiHandlerPlugin);
                        }
                        //ini.putService(item.getKey(), pi);
                    } catch (Exception xx) {
                        //noinspection SuspiciousMethodCalls
                        protocolServersCache.remove(item);
                        throw new RuntimeException(xx);
                    }
                } catch (Exception ex) {
                    log.error("Unable to start protocol {}", item.getKey(), ex);
                }

                latch.countDown();
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            log.error("Error waiting for plugin to start");
        }

        var globalPlugins = diService.getInstances(GlobalPluginDescriptor.class);
        for (int i = globalPlugins.size() - 1; i >= 0; i--) {
            var plugin = globalPlugins.get(i);
            var pluginSettings = ini.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (pluginSettings != null) {
                plugin.initialize(ini, pluginSettings);
            } else {
                globalPlugins.remove(i);
            }
        }
        new Thread(() -> {
            try {
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
