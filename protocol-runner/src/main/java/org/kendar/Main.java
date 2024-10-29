package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.cli.*;
import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.AmqpStorageHandler;
import org.kendar.mongo.MongoProtocol;
import org.kendar.mongo.MongoProxy;
import org.kendar.mongo.MongoStorageHandler;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.MqttStorageHandler;
import org.kendar.mysql.MySqlStorageHandler;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.Resp3StorageHandler;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcStorageHandler;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Main {
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
                var ini = new Ini();

                ini.load(Path.of(configFile).toAbsolutePath().toFile());
                for(var key:ini.getSections()) {


                    try {
                        var replayFromLog = ini.getValue(key,"shouldreplay",Boolean.class,false);
                        var protocol = ini.getValue(key,"protocol",String.class);
                        var portVal =  ini.getValue(key,"listen",String.class);
                        var login = ini.getValue(key,"login",String.class);
                        var password = ini.getValue(key,"password",String.class);
                        var logLevel = ini.getValue(key,"loglevel",String.class,"ERROR");
                        var logsDir = ini.getValue(key,"datadir",String.class);
                        var callDurationTimes = ini.getValue(key,"respectcallduration",Boolean.class,false);
                        var jdbcForcedSchema = ini.getValue(key,"forcejdbcschema",String.class);
                        var jdbcReplaceQueries = ini.getValue(key,"replacequery",String.class);
                        var timeout = ini.getValue(key,"timeout",Integer.class,30);
                        var connectionString = ini.getValue(key,"connection",String.class);


                        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

                        var logger = loggerContext.getLogger("org.kendar");
                        logger.setLevel(Level.toLevel(logLevel, Level.ERROR));
                        new Thread(()->{
                            try {
                                System.out.println("STARTING "+key);
                                runWithParams(key, stopWhenFalse, timeout, logLevel, protocol, replayFromLog, portVal,
                                    jdbcForcedSchema, login, password, jdbcReplaceQueries,
                                    callDurationTimes,connectionString,logsDir);
                            } catch (Exception ex) {
                                System.out.println(ex);
                                throw new RuntimeException(ex);
                            }
                        }).start();

                    }catch (Exception ex){
                        System.out.println(ex);
                    }
                }
            } else {
                var replayFromLog = cmd.hasOption("pl");
                var protocol = cmd.getOptionValue("p");
                var portVal = cmd.getOptionValue("l");
                var login = cmd.getOptionValue("xl");
                var password = cmd.getOptionValue("xw");
                var logLevel = cmd.getOptionValue("v");
                var callDurationTimes = cmd.hasOption("cdt");
                var jdbcForcedSchema = cmd.getOptionValue("js");
                var jdbcReplaceQueries = cmd.getOptionValue("jr");
                var connectionString = cmd.getOptionValue("xc");
                var timeout = cmd.getOptionValue("t");
                var logsDir = cmd.getOptionValue("xd");
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

                runWithParams("DEFAULT",stopWhenFalse, timeoutSec, logLevel, protocol, replayFromLog, portVal,
                        jdbcForcedSchema, login, password, jdbcReplaceQueries,
                        callDurationTimes,connectionString,logsDir);
            }
        } catch (Exception ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }
    }

    private static void runWithParams(String id, Supplier<Boolean> stopWhenFalse, int timeoutSec, String logLevel,
                                      String protocol, boolean replayFromLog,
                                      String portVal, String jdbcForcedSchema, String login, String password,
                                      String jdbcReplaceQueries, boolean callDurationTimes, String connectionString, String logsDir) throws Exception {
        //ProtoContext.setTimeout(timeoutSec);



        if (logsDir != null && !logsDir.isEmpty()) {
            logsDir = logsDir.replace("{timestamp}", "" + new Date().getTime());
            logsDir = logsDir.replace("{protocol}", protocol.trim().toLowerCase());
        }
        if (replayFromLog &&
                (logsDir == null || logsDir.isEmpty())) {
            throw new Exception("cannot replay, missing logsDir (xd)");
        }
        StorageRepository storage = new NullStorageRepository();
        if (logsDir != null && !logsDir.isEmpty()) {
            storage = new FileStorageRepository(Path.of(logsDir));
        }
        var port = -1;
        if (portVal != null && !portVal.isEmpty()) {
            port = Integer.parseInt(portVal);
        }
        if (protocol.equalsIgnoreCase("mysql")) {
            if (port == -1) port = 3306;
            runMysql(id,port, storage, connectionString, jdbcForcedSchema, login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec);
        } else if (protocol.equalsIgnoreCase("postgres")) {
            if (port == -1) port = 5432;
            runPostgres(id,port, storage, connectionString, jdbcForcedSchema, login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec);
        } else if (protocol.equalsIgnoreCase("mongo")) {
            if (port == -1) port = 27017;
            runMongo(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec);
        } else if (protocol.equalsIgnoreCase("amqp091")) {
            if (port == -1) port = 5672;
            runAmqp091(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec);
        } else if (protocol.equalsIgnoreCase("redis")) {
            if (port == -1) port = 6379;
            runRedis(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec);
        } else if (protocol.equalsIgnoreCase("mqtt")) {
            if (port == -1) port = 1883;
            runMqtt(id,port, storage, connectionString, login, password, replayFromLog, callDurationTimes,timeoutSec);
        } else {
            throw new Exception("missing protocol (p)");
        }

        while (stopWhenFalse.get()) {
            Sleeper.sleep(100);
        }
        protocolServer.get(id).stop();

        System.out.println("EXITED");
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
        execute(args, Main::stopWhenQuitCommand);
    }


    private static void runPostgres(String id, int port, StorageRepository logsDir, String connectionString, String forcedSchema,
                                    String login, String password, boolean replayFromLog, String jdbcReplaceQueries, boolean callDurationTimes, int timeoutSec) throws IOException {
        runJdbc(id,"postgres", "org.postgresql.Driver", port, logsDir, connectionString, forcedSchema,
                login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec);
    }

    private static void runMysql(String id, int port, StorageRepository logsDir, String connectionString, String forcedSchema,
                                 String login, String password, boolean replayFromLog, String jdbcReplaceQueries, boolean callDurationTimes, int timeoutSec) throws IOException {
        runJdbc(id,"mysql", "com.mysql.cj.jdbc.Driver", port, logsDir, connectionString, forcedSchema,
                login, password, replayFromLog, jdbcReplaceQueries, callDurationTimes,timeoutSec);
    }

    private static void runJdbc(String id,String type, String driver, int port, StorageRepository logsDir,
                                String connectionString, String forcedSchema,
                                String login, String password, boolean replayFromLog, String jdbcReplaceQueries,
                                boolean callDurationTimes,int timeoutSec) throws IOException {
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

    private static void runMongo(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec) {
        var baseProtocol = new MongoProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new MongoProxy(connectionString);

        if (replayFromLog) {
            proxy = new MongoProxy(new MongoStorageHandler(logsDir));
        } else {
            proxy.setStorage(new MongoStorageHandler(logsDir));
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void runAmqp091(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec) {
        var baseProtocol = new AmqpProtocol(port);
        baseProtocol.setTimeout(timeoutSec);
        var proxy = new AmqpProxy(connectionString, login, password);

        if (replayFromLog) {
            proxy = new AmqpProxy();
            proxy.setStorage(new AmqpStorageHandler(logsDir));
        } else {
            proxy.setStorage(new AmqpStorageHandler(logsDir));
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void runRedis(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec) {
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
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var ps = new TcpServer(baseProtocol);
        ps.useCallDurationTimes(callDurationTimes);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(id,ps);
    }

    private static void runMqtt(String id, int port, StorageRepository logsDir, String connectionString, String login, String password, boolean replayFromLog, boolean callDurationTimes, int timeoutSec) {
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

}