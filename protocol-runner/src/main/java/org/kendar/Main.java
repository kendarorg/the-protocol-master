package org.kendar;

import org.apache.commons.cli.*;
import org.kendar.amqp.v09.AmqpFileStorage;
import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.mongo.MongoFileStorage;
import org.kendar.mongo.MongoProtocol;
import org.kendar.mongo.MongoProxy;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.JdbcReplayProxy;
import org.kendar.sql.jdbc.storage.JdbcFileStorage;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Supplier;

public class Main {
    private static TcpServer protocolServer;

    public static void execute(String[] args, Supplier<Boolean> stopWhenFalse) {

        Options options = new Options();
        options.addOption("p", true, "Select protocol (mysql/mongo/postgres/amqp091)");
        options.addOption("l", true, "Select listening port");
        options.addOption("xl", true, "Select remote login");
        options.addOption("xw", true, "Select remote password");
        options.addOption("xc", true, "Select remote connection string");
        options.addOption("xd", true, "Select remote log directory (you can set a {timestamp} value\n" +
                "that will be replaced with the current timestamp)");
        options.addOption("pl", false, "Replay from log directory");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            var replayFromLog = cmd.hasOption("pl");
            var protocol = cmd.getOptionValue("p");
            var portVal = cmd.getOptionValue("l");
            var login = cmd.getOptionValue("xl");
            var password = cmd.getOptionValue("xw");
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
                runMysql(port, logsDir, connectionString, login, password, replayFromLog);
            } else if (protocol.equalsIgnoreCase("postgres")) {
                if (port == -1) port = 5432;
                runPostgres(port, logsDir, connectionString, login, password, replayFromLog);
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
                Thread.yield();
            }
        } catch (Exception ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }
    }

    private static Boolean stopWhenQuitCommand() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press Q to quit");
        String line = scanner.nextLine();
        try {
            if (line != null && line.trim().equalsIgnoreCase("q")) {
                System.out.println("Exiting");
                protocolServer.stop();
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


    private static void runPostgres(int port, String logsDir, String connectionString,
                                    String login, String password, boolean replayFromLog) {
        runJdbc("org.postgresql.Driver", port, logsDir, connectionString, login, password, replayFromLog);
    }

    private static void runMysql(int port, String logsDir, String connectionString,
                                 String login, String password, boolean replayFromLog) {
        runJdbc("com.mysql.cj.jdbc.Driver", port, logsDir, connectionString, login, password, replayFromLog);
    }

    private static void runJdbc(String driver, int port, String logsDir,
                                String connectionString, String login, String password, boolean replayFromLog) {
        var baseProtocol = new PostgresProtocol(port);
        var proxy = new JdbcProxy(driver,
                connectionString,
                login, password);
        if (logsDir != null) {
            if (replayFromLog) {
                proxy = new JdbcReplayProxy(new JdbcFileStorage(Path.of(logsDir)));
            } else {
                proxy.setStorage(new JdbcFileStorage(Path.of(logsDir)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);
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