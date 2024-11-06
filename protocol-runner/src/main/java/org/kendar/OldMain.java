package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.cli.*;
import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.AmqpStorageHandler;
import org.kendar.filters.FilterDescriptor;
import org.kendar.http.MasterHandler;
import org.kendar.http.plugins.ErrorFilter;
import org.kendar.http.plugins.GlobalFilter;
import org.kendar.http.plugins.MockFilter;
import org.kendar.http.plugins.RecordingFilter;
import org.kendar.http.utils.ConnectionBuilderImpl;
import org.kendar.http.utils.callexternal.ExternalRequesterImpl;
import org.kendar.http.utils.converters.RequestResponseBuilderImpl;
import org.kendar.http.utils.dns.DnsMultiResolverImpl;
import org.kendar.http.utils.filters.FilteringClassesHandlerImpl;
import org.kendar.http.utils.rewriter.RemoteServerStatus;
import org.kendar.http.utils.rewriter.SimpleRewriterConfig;
import org.kendar.http.utils.rewriter.SimpleRewriterHandlerImpl;
import org.kendar.http.utils.ssl.CertificatesManager;
import org.kendar.http.utils.ssl.FileResourcesUtils;
import org.kendar.mongo.MongoProtocol;
import org.kendar.mongo.MongoProxy;
import org.kendar.mongo.MongoStorageHandler;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.MqttStorageHandler;
import org.kendar.mysql.MySqlStorageHandler;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.proxy.ProxyServer;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.Resp3StorageHandler;
import org.kendar.server.KendarHttpsServer;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcStorageHandler;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;
import org.pf4j.JarPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class OldMain {
    private static final Logger log = LoggerFactory.getLogger(OldMain.class);
    private static ConcurrentHashMap<String,TcpServer> protocolServer = new ConcurrentHashMap<>();

    public static boolean isRunning(String key) {
        if (!protocolServer.containsKey(key.toUpperCase())) return false;
        return protocolServer.get(key.toUpperCase()).isRunning();
    }

    public static boolean isRunning() {
        return protocolServer.values().stream().anyMatch(v->v.isRunning());
    }

    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) {
        if(isRunning("DEFAULT")){
            protocolServer.get("DEFAULT").stop();
        }
        Options options = getOptions();

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            var configFile = cmd.getOptionValue("cfg");
            if (configFile != null){
                runWithConfig(stopWhenFalse, configFile);
            } else {
                runWithCommand(stopWhenFalse, cmd);
            }
        } catch (Exception ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }
    }

    private static void runWithCommand(Supplier<Boolean> stopWhenFalse, CommandLine cmd) throws Exception {
        var replayFromLog = cmd.hasOption("pl");
        var protocol = cmd.getOptionValue("p");
        var portVal = cmd.getOptionValue("l");
        var login = cmd.getOptionValue("xl");
        var password = cmd.getOptionValue("xw");
        var logLevel = cmd.getOptionValue("v");
        var callDurationTimes = cmd.hasOption("cdt");
        var jdbcForcedSchema = cmd.getOptionValue("js");
        var jdbcReplaceQueries = cmd.getOptionValue("jr");
        var logsDir = cmd.getOptionValue("xd");
        var connectionString = cmd.getOptionValue("xc");
        var timeout = cmd.getOptionValue("t");
        var pluginsDir = cmd.getOptionValue("pl");

        var filters = loadFilters(pluginsDir);

        StorageRepository storage = setupStorage(logsDir);

        if (logLevel == null || logLevel.isEmpty()) {
            logLevel = "ERROR";
        }
        var timeoutSec = 30;
        if (timeout != null && !timeout.isEmpty() && Pattern.matches("[0-9]+", timeout)) {
            timeoutSec = Integer.parseInt(timeout);
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        var logger = loggerContext.getLogger("org.kendar");
        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));

        runWithParams("DEFAULT", stopWhenFalse, timeoutSec,  protocol, replayFromLog, portVal,
                jdbcForcedSchema, login, password, jdbcReplaceQueries,
                callDurationTimes,connectionString,storage,filters, null);
    }



    private static HashMap<String, List<FilterDescriptor>> loadFilters(String pluginsDir) {
        if(!Path.of(pluginsDir).toAbsolutePath().toFile().exists()){
            return new HashMap<>();
        }
        var pluginManager = new JarPluginManager(Path.of(pluginsDir).toAbsolutePath());
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        var filters = new HashMap<String, List<FilterDescriptor>>();
        for(var item : pluginManager.getExtensions(FilterDescriptor.class)){
            var protocol = item.getProtocol().toLowerCase();
            if(!filters.containsKey(protocol)){
                filters.put(protocol,new ArrayList<>());
            }
            filters.get(protocol).add(item);
        }
        return filters;
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

    private static void runWithParams(String id, Supplier<Boolean> stopWhenFalse, int timeoutSec,
                                      String protocol, boolean replayFromLog,
                                      String portVal, String jdbcForcedSchema, String login, String password,
                                      String jdbcReplaceQueries, boolean callDurationTimes, String connectionString,
                                      StorageRepository storage, HashMap<String, List<FilterDescriptor>> allFilters, Ini ini) throws Exception {
        //ProtoContext.setTimeout(timeoutSec);


        var filters = loadCorrectFiltersForProtocol(id, protocol, allFilters, ini);
        if (replayFromLog &&
                (storage instanceof NullStorageRepository)) {
            throw new Exception("cannot replay, missing logsDir (xd)");
        }
        var port = -1;
        if (portVal != null && !portVal.isEmpty()) {
            port = Integer.parseInt(portVal);
        }
        if (protocol.equalsIgnoreCase("mysql")) {
            if (port == -1) port = 3306;
            runMysql(id,port, storage, connectionString, jdbcForcedSchema, login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec,filters);
        } else if (protocol.equalsIgnoreCase("postgres")) {
            if (port == -1) port = 5432;
            runPostgres(id,port, storage, connectionString, jdbcForcedSchema, login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec,filters);
        } else if (protocol.equalsIgnoreCase("mongo")) {
            if (port == -1) port = 27017;
            runMongo(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec,filters);
        } else if (protocol.equalsIgnoreCase("amqp091")) {
            if (port == -1) port = 5672;
            runAmqp091(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec,filters);
        } else if (protocol.equalsIgnoreCase("redis")) {
            if (port == -1) port = 6379;
            runRedis(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec,filters);
        } else if (protocol.equalsIgnoreCase("mqtt")) {
            if (port == -1) port = 1883;
            runMqtt(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec,filters);
        } else {
            throw new Exception("missing protocol (p)");
        }

        while (stopWhenFalse.get()) {
            Sleeper.sleep(100);
        }
        protocolServer.get(id).stop();

        System.out.println("EXITED");
    }

    private static ArrayList<FilterDescriptor> loadCorrectFiltersForProtocol(String id, String protocol, HashMap<String, List<FilterDescriptor>> allFilters, Ini ini) {
        var availableFilters = allFilters.get(protocol.toLowerCase());
        if(availableFilters==null)availableFilters=new ArrayList<>();
        var filters = new ArrayList<FilterDescriptor>();
        if(ini !=null) {

            for (var availableFilter : availableFilters) {
                var sectionId = id +"-"+availableFilter.getId();
                var section = ini.getSection(sectionId);
                if(section!=null && !section.isEmpty()){
                    var clonedFilter = availableFilter.clone();
                    filters.add(clonedFilter);
                }
            }
        }
        return filters;
    }


    private static Options getOptions() {
        Options options = new Options();
        options.addOption("cfg", true, "Load config file");
        options.addOption("p", true, "Select protocol (mysql/mongo/postgres/amqp091/redis)");
        options.addOption("l", true, "[all] Select listening port");

        options.addOption("xl", true, "[mysql/mongo/postgres/amqp091/mqtt] Select remote login");
        options.addOption("xw", true, "[mysql/mongo/postgres/amqp091/mqtt] Select remote password");
        options.addOption("xc", true, "[all] Select remote connection string (for redis use redis://host:port");
        options.addOption("xd", true, "[all] Select log/replay directory "); //(or id when not using files but db/web)
        options.addOption("lc", true, "[all] The log type: [file]"); ///db:connectionstring/rest:host:port
        options.addOption("pl", false, "[all] Replay from log/replay source.");
        options.addOption("plid", true, "[all] Set an id for the replay instance (default to timestamp_uuid).");
        options.addOption("v", true, "[all] Log level (default ERROR)");
        options.addOption("t", true, "[all] Set timeout in seconds towards proxied system (default 30s)");
        options.addOption("cdt", false, "[all] Respect call duration timing");
        options.addOption("js", true, "[jdbc] Set schema");
        options.addOption("jr", true, "[jdbc] Replace queries");
        options.addOption("pl", true, "[all] Plugins dir (default plugins)");
        return options;
    }

    private static Boolean stopWhenQuitCommand() {
        Scanner scanner = new Scanner(System.in);
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

    public static void main(String[] args) {
        execute(args, OldMain::stopWhenQuitCommand);
    }


    private static void runPostgres(String id, int port, StorageRepository logsDir, String connectionString, String forcedSchema,
                                    String login, String password, boolean replayFromLog, String jdbcReplaceQueries, boolean callDurationTimes, int timeoutSec, List<FilterDescriptor> filters) throws IOException {
        runJdbc(id,"postgres", "org.postgresql.Driver", port, logsDir, connectionString, forcedSchema,
                login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec,filters);
    }

    private static void runMysql(String id, int port, StorageRepository logsDir, String connectionString, String forcedSchema,
                                 String login, String password, boolean replayFromLog, String jdbcReplaceQueries, boolean callDurationTimes, int timeoutSec,
                                 List<FilterDescriptor> filters) throws IOException {
        runJdbc(id,"mysql", "com.mysql.cj.jdbc.Driver", port, logsDir, connectionString, forcedSchema,
                login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec,filters);
    }

    private static void runJdbc(String id,String type, String driver, int port, StorageRepository logsDir,
                                String connectionString, String forcedSchema,
                                String login, String password, boolean replayFromLog, String jdbcReplaceQueries,
                                boolean callDurationTimes,int timeoutSec,
                                List<FilterDescriptor> filters) throws IOException {
        var baseProtocol = new PostgresProtocol(port);
        var proxy = new JdbcProxy(driver,
                connectionString, forcedSchema,
                login, password);

        JdbcStorageHandler storage = new JdbcStorageHandler(logsDir);
        if (type.equalsIgnoreCase("mysql")) {
            storage = new MySqlStorageHandler(logsDir);
        }
        if (replayFromLog) {
            proxy = new JdbcProxy(storage);
        } else {
            proxy.setStorage(storage);
        }
        proxy.setFilters(filters);
        if (jdbcReplaceQueries != null && !jdbcReplaceQueries.isEmpty() && Files.exists(Path.of(jdbcReplaceQueries))) {

            handleReplacementQueries(jdbcReplaceQueries, proxy);
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.setTimeout(timeoutSec);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void handleReplacementQueries(String jdbcReplaceQueries, JdbcProxy proxy) throws IOException {
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

    private static void runMongo(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec, List<FilterDescriptor> filters) {
        var baseProtocol = new MongoProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MongoProxy(connectionString);

        if (replayFromLog) {
            proxy = new MongoProxy(new MongoStorageHandler(logsDir));
        } else {
            proxy.setStorage(new MongoStorageHandler(logsDir));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void runAmqp091(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec, List<FilterDescriptor> filters) {
        var baseProtocol = new AmqpProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new AmqpProxy(connectionString, login, password);

        if (replayFromLog) {
            proxy = new AmqpProxy();
            proxy.setStorage(new AmqpStorageHandler(logsDir));
        } else {
            proxy.setStorage(new AmqpStorageHandler(logsDir));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void runRedis(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec, List<FilterDescriptor> filters) {
        var baseProtocol = new Resp3Protocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new Resp3Proxy(connectionString, login, password);

        if (replayFromLog) {
            proxy = new Resp3Proxy();
            proxy.setStorage(new Resp3StorageHandler(logsDir) {
            });
        } else {
            proxy.setStorage(new Resp3StorageHandler(logsDir));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void runMqtt(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec, List<FilterDescriptor> filters) {
        var baseProtocol = new MqttProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MqttProxy(connectionString, login, password);

        if (replayFromLog) {
            proxy = new MqttProxy();
            proxy.setStorage(new MqttStorageHandler(logsDir) {
            });
        } else {
            proxy.setStorage(new MqttStorageHandler(logsDir));
        }
        proxy.setFilters(filters);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    public static void stop() {
        for(var server:protocolServer.values()){
            server.stop();
        }
    }

    private static void runWithConfig(Supplier<Boolean> stopWhenFalse, String configFile) throws IOException {
        var ini = new Ini();
        ini.load(Path.of(configFile).toAbsolutePath().toFile());
        var logsDir = ini.getValue("global","datadir",String.class);
        StorageRepository storage = setupStorage(logsDir);

        var pluginsDir = ini.getValue("global", "pluginsDir", String.class, "plugins");
        var filters = loadFilters(pluginsDir);
        var logLevel = ini.getValue("global", "loglevel", String.class, "ERROR");

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var logger = loggerContext.getLogger("org.kendar");
        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));

        for(var key:ini.getSections()) {
            try {
                var protocol = ini.getValue(key,"protocol",String.class);
                if(protocol==null || protocol.isEmpty())continue;

                if(!protocol.equalsIgnoreCase("http")) {
                    runWithConfigNonHttp(stopWhenFalse, key, ini, storage, protocol, filters);
                }else{
                    runWithConfigHttp(stopWhenFalse,key,ini,storage,protocol,filters);
                }
            }catch (Exception ex){
                System.out.println(ex);
            }
        }
    }

    private static void runWithConfigNonHttp(Supplier<Boolean> stopWhenFalse, String key, Ini ini, StorageRepository storage, String protocol, HashMap<String, List<FilterDescriptor>> filters) {
        var replayFromLog = ini.getValue(key, "replay", Boolean.class, false);
        var portVal = ini.getValue(key, "listen", String.class);
        var login = ini.getValue(key, "login", String.class);
        var password = ini.getValue(key, "password", String.class);

        var callDurationTimes = ini.getValue(key, "respectcallduration", Boolean.class, false);
        var jdbcForcedSchema = ini.getValue(key, "forcejdbcschema", String.class);
        var jdbcReplaceQueries = ini.getValue(key, "replacequery", String.class);
        var timeout = ini.getValue(key, "timeout", Integer.class, 30);
        var connectionString = ini.getValue(key, "connection", String.class);




        var finalStorage = storage;
        new Thread(() -> {
            try {
                System.out.println("STARTING " + key);
                runWithParams(key, stopWhenFalse, timeout, protocol, replayFromLog, portVal,
                        jdbcForcedSchema, login, password, jdbcReplaceQueries,
                        callDurationTimes, connectionString, finalStorage, filters,ini);
            } catch (Exception ex) {
                System.out.println(ex);
                throw new RuntimeException(ex);
            }
        }).start();
    }

    private static SimpleRewriterConfig loadRewritersConfiguration(String key, Ini ini) {
        var proxyConfig = new SimpleRewriterConfig();
        for (var id = 0; id < 255; id++) {
            var when = ini.getValue(key+"-rewriter", "rewrite." + id + ".when", String.class);
            var where = ini.getValue(key+"-rewriter", "rewrite." + id + ".where", String.class);
            var test = ini.getValue(key+"-rewriter", "rewrite." + id + ".test", String.class);
            if (when == null || where == null) {
                continue;
            }
            var remoteServerStatus = new RemoteServerStatus(id + "",
                    when,
                    where,
                    test);
            if(test==null||test.isEmpty()) {
                remoteServerStatus.setRunning(true);
                remoteServerStatus.setForce(true);
            }else{

                remoteServerStatus.setRunning(false);
                remoteServerStatus.setForce(false);
            }
            proxyConfig.getProxies().add(remoteServerStatus);
        }
        return proxyConfig;
    }

    private static HttpsServer createHttpsServer(CertificatesManager certificatesManager, InetSocketAddress sslAddress, int backlog, String cname, String der, String key) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, List.of(),cname, der, key);
        return httpsServer;
    }

    private static void runWithConfigHttp(Supplier<Boolean> stopWhenFalseFunction, String sectionKey, Ini ini, StorageRepository storage, String protocol, HashMap<String, List<FilterDescriptor>> allFilters) throws Exception {
        var filters = loadCorrectFiltersForProtocol(sectionKey, protocol, allFilters, ini);
        AtomicBoolean stopWhenFalse = new AtomicBoolean(true);
        var port = ini.getValue(sectionKey, "http.port", Integer.class, 8085);
        var httpsPort = ini.getValue(sectionKey, "https.port", Integer.class, port + 400);
        var proxyPort = ini.getValue(sectionKey, "port.proxy", Integer.class, 9999);
        log.info("LISTEN HTTP: " + port);
        log.info("LISTEN HTTPS: " + httpsPort);
        log.info("LISTEN PROXY: " + proxyPort);
        var backlog = 60;
        var useCachedExecutor = true;
        var address = new InetSocketAddress(port);
        var sslAddress = new InetSocketAddress(httpsPort);


        // initialise the HTTP server
        var proxyConfig = loadRewritersConfiguration(sectionKey,ini);
        var dnsHandler = new DnsMultiResolverImpl();
        var connectionBuilder = new ConnectionBuilderImpl(dnsHandler);
        var requestResponseBuilder = new RequestResponseBuilderImpl();

        var certificatesManager = new CertificatesManager(new FileResourcesUtils());
        var httpServer = HttpServer.create(address, backlog);

        var der = ini.getValue(sectionKey+"-ssl", "der", String.class, "resources://certificates/ca.der");
        var key = ini.getValue(sectionKey+"-ssl", "key", String.class, "resources://certificates/ca.key");
        var cname = ini.getValue(sectionKey+"-ssl", "cname", String.class,"C=US,O=Local Development,CN=local.org");

        var httpsServer = createHttpsServer(certificatesManager,sslAddress, backlog,cname, der, key);


        var proxy = new ProxyServer(proxyPort)
                .withHttpRedirect(port).withHttpsRedirect(httpsPort)
                .withDnsResolver(host -> {
                    try {
                        certificatesManager.setupSll(httpsServer, List.of(host),cname, der, key);
                    } catch (Exception e) {
                        return host;
                    }
                    return "127.0.0.1";
                }).
                ignoringHosts("static.chartbeat.com").
                ignoringHosts("detectportal.firefox.com").
                ignoringHosts("firefox.settings.services.mozilla.com").
                ignoringHosts("incoming.telemetry.mozilla.org").
                ignoringHosts("push.services.mozilla.com");
        new Thread(proxy).start();

        var globalFilter = new GlobalFilter();

        filters.add(globalFilter);
        filters.add(new RecordingFilter());
        filters.add(new ErrorFilter());
        filters.add(new MockFilter());
        for (var i = filters.size() - 1; i >= 0; i--) {
            var filter = filters.get(i);
            var section = ini.getSection(sectionKey+"-"+filter.getId());
            if (!filter.getId().equalsIgnoreCase("global") &&
                    !ini.getValue(sectionKey+"-"+filter.getId(), "active", Boolean.class, false)) {
                filters.remove(i);
                continue;
            }
            log.info("EXTENSION: " + filter.getId());
            filter.initialize(section);
        }
        globalFilter.setFilters(filters);
        globalFilter.setServer(httpServer, httpsServer);
        globalFilter.setShutdownVariable(stopWhenFalse);
        var handler = new MasterHandler(
                new FilteringClassesHandlerImpl(filters),
                new SimpleRewriterHandlerImpl(proxyConfig, dnsHandler),
                new RequestResponseBuilderImpl(),
                new ExternalRequesterImpl(requestResponseBuilder, dnsHandler, connectionBuilder),
                connectionBuilder);

        httpServer.createContext("/", handler);
        httpsServer.createContext("/", handler);
        if (useCachedExecutor) {
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpsServer.setExecutor(Executors.newCachedThreadPool());
        } else {
            httpServer.setExecutor(null); // creates a default executor
            httpsServer.setExecutor(null);
        }
        httpsServer.start();
        httpServer.start();
        new Thread(()->{
            while(stopWhenFalseFunction.get()){
                Sleeper.sleep(100);
            }
            stopWhenFalse.set(false);
        }).start();
    }

}