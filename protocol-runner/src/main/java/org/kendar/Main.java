package org.kendar;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.cli.*;
import org.kendar.amqp.v09.AmqpFileStorage;
import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.mongo.MongoFileStorage;
import org.kendar.mongo.MongoProtocol;
import org.kendar.mongo.MongoProxy;
import org.kendar.mysql.MySqlFileStorage;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcFileStorage;
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Main {
    private static TcpServer protocolServer;

    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) {

        Options options = new Options();
        options.addOption("p", true, "Select protocol (mysql/mongo/postgres/amqp091)");
        options.addOption("l", true, "Select listening port");
        options.addOption("xl", true, "Select remote login");
        options.addOption("xw", true, "Select remote password");
        options.addOption("xc", true, "Select remote connection string");
        options.addOption("xd", true, "Select log/replay directory (you can set a {timestamp} value\n" +
                "that will be replaced with the current timestamp)");
        options.addOption("pl", false, "Replay from log/replay directory");
        options.addOption("v", true, "Log level (default ERROR)");
        options.addOption("t", true, "Set timeout in seconds towards proxied system (default 30s)");
        options.addOption("js", true, "[jdbc] Set schema");
        options.addOption("jr", true, "[jdbc] Replace queries");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            var replayFromLog = cmd.hasOption("pl");
            var protocol = cmd.getOptionValue("p");
            var portVal = cmd.getOptionValue("l");
            var login = cmd.getOptionValue("xl");
            var password = cmd.getOptionValue("xw");
            var logLevel = cmd.getOptionValue("v");
            var jdbcForcedSchema = cmd.getOptionValue("js");
            var jdbcReplaceQueries = cmd.getOptionValue("jr");
            var timeout = cmd.getOptionValue("t");
            if (logLevel == null || logLevel.isEmpty()) {
                logLevel = "ERROR";
            }
            var timeoutSec = 30;
            if (timeout != null && !timeout.isEmpty() && Pattern.matches("[0-9]+",timeout)) {
                timeoutSec = Integer.parseInt(timeout);
            }

            //ProtoContext.setTimeout(timeoutSec);

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            var logger = loggerContext.getLogger("org.kendar");
            logger.setLevel(Level.toLevel(logLevel, Level.ERROR));

            var connectionString = cmd.getOptionValue("xc");
            var logsDir = cmd.getOptionValue("xd");
            if (logsDir != null && !logsDir.isEmpty()) {
                logsDir = logsDir.replace("{timestamp}", "" + new Date().getTime());
            }
            if (replayFromLog &&
                    (logsDir == null || logsDir.isEmpty())) {
                throw new Exception("cannot replay, missing logsDir (xd)");
            }
            var port = -1;
            if (portVal != null && !portVal.isEmpty()) {
                port = Integer.parseInt(portVal);
            }
            if (protocol.equalsIgnoreCase("mysql")) {
                if (port == -1) port = 3306;
                runMysql(port, logsDir, connectionString, jdbcForcedSchema, login, password, replayFromLog, jdbcReplaceQueries);
            } else if (protocol.equalsIgnoreCase("postgres")) {
                if (port == -1) port = 5432;
                runPostgres(port, logsDir, connectionString, jdbcForcedSchema, login, password, replayFromLog, jdbcReplaceQueries);
            } else if (protocol.equalsIgnoreCase("mongo")) {
                if (port == -1) port = 27017;
                runMongo(port, logsDir, connectionString, login, password, replayFromLog);
            } else if (protocol.equalsIgnoreCase("amqp091")) {
                if (port == -1) port = 5672;
                runAmqp091(port, logsDir, connectionString, login, password, replayFromLog);
            } else {
                throw new Exception("missing protocol (p)");
            }
            while (stopWhenFalse.get()) {
                Sleeper.sleep(100);
            }
            protocolServer.stop();
        } catch (Exception ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }
        System.out.println("EXITED");
    }

    private static Boolean stopWhenQuitCommand() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press Q to quit");
        String line = scanner.nextLine();
        try {
            if (line != null && line.trim().equalsIgnoreCase("q")) {
                System.out.println("Exiting");
                return false;
            } else {
                System.out.println("Command not recognized: " + line.trim());
                return true;
            }
        } catch (Exception ex) {
            System.out.println("Exiting");
            System.err.println(ex);
            return false;
        }
    }

    public static void main(String[] args) {
        execute(args, Main::stopWhenQuitCommand);
    }


    private static void runPostgres(int port, String logsDir, String connectionString, String forcedSchema,
                                    String login, String password, boolean replayFromLog, String jdbcReplaceQueries) throws IOException {
        runJdbc("postgres", "org.postgresql.Driver", port, logsDir, connectionString, forcedSchema,
                login, password, replayFromLog, jdbcReplaceQueries);
    }

    private static void runMysql(int port, String logsDir, String connectionString, String forcedSchema,
                                 String login, String password, boolean replayFromLog, String jdbcReplaceQueries) throws IOException {
        runJdbc("mysql", "com.mysql.cj.jdbc.Driver", port, logsDir, connectionString, forcedSchema,
                login, password, replayFromLog, jdbcReplaceQueries);
    }

    private static void runJdbc(String type, String driver, int port, String logsDir,
                                String connectionString, String forcedSchema,
                                String login, String password, boolean replayFromLog, String jdbcReplaceQueries) throws IOException {
        var baseProtocol = new PostgresProtocol(port);
        var proxy = new JdbcProxy(driver,
                connectionString, forcedSchema,
                login, password);

        if (logsDir != null) {
            var logsDirPath = Path.of(logsDir);
            JdbcFileStorage storage = new JdbcFileStorage(logsDirPath);
            if (type.equalsIgnoreCase("mysql")) {
                storage = new MySqlFileStorage(logsDirPath);
            }
            if (replayFromLog) {
                proxy = new JdbcProxy(storage);
            } else {
                proxy.setStorage(storage);
            }
        }
        if (jdbcReplaceQueries != null && !jdbcReplaceQueries.isEmpty() && Files.exists(Path.of(jdbcReplaceQueries))) {

            handleReplacementQueries(jdbcReplaceQueries, (JdbcProxy) proxy);
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);
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
        if(replacerItem.getToFind()!=null && !replacerItem.getToFind().isEmpty()){
            items.add(replacerItem);
        }
        proxy.setQueryReplacement(items);
    }

    private static void runMongo(int port, String logsDir, String connectionString, String login, String password, boolean replayFromLog) {
        var baseProtocol = new MongoProtocol(port);
        var proxy = new MongoProxy(connectionString);
        if (logsDir != null) {
            if (replayFromLog) {
                proxy = new MongoProxy(new MongoFileStorage(Path.of(logsDir)));
            } else {
                proxy.setStorage(new MongoFileStorage(Path.of(logsDir)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);
    }

    private static void runAmqp091(int port, String logsDir, String connectionString, String login, String password, boolean replayFromLog) {
        var baseProtocol = new AmqpProtocol(port);
        var proxy = new AmqpProxy(connectionString, login, password);
        if (logsDir != null) {
            if (replayFromLog) {
                proxy = new AmqpProxy();
                proxy.setStorage(new AmqpFileStorage(Path.of(logsDir)));
            } else {
                proxy.setStorage(new AmqpFileStorage(Path.of(logsDir)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);
    }
}