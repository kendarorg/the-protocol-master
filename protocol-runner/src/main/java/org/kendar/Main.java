package org.kendar;

import org.apache.commons.cli.*;
import org.kendar.mongo.MongoFileStorage;
import org.kendar.mongo.MongoProtocol;
import org.kendar.mongo.MongoProxy;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.protocol.Sleeper;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcFileStorage;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    private static TcpServer protocolServer;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Options options = new Options();
        options.addOption("p", true, "Select protocol");
        options.addOption("l", true, "Select port");
        options.addOption("xl", true, "Select proxy login");
        options.addOption("xw", true, "Select proxy password");
        options.addOption("xc", true, "Select proxy connection string");
        options.addOption("xd", true, "Select proxy log directory");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            var protocol = cmd.getOptionValue("p");
            var port = Integer.getInteger(cmd.getOptionValue("l")).intValue();
            var login = cmd.getOptionValue("xl");
            var password = cmd.getOptionValue("xw");
            var connectionString = cmd.getOptionValue("xc");
            var logsDir = cmd.getOptionValue("xd");
            if(protocol.equalsIgnoreCase("mysql")){
                runMysql(port,logsDir,connectionString,login,password);
            }else if(protocol.equalsIgnoreCase("postgres")){
                runPostgres(port,logsDir,connectionString,login,password);
            }else if(protocol.equalsIgnoreCase("mongo")){
                runMongo(port,logsDir,connectionString,login,password);
            }else{
                throw new Exception("missing protocol");
            }
            while(true) {
                System.out.println("Press Q to quit");
                String line = scanner.nextLine();
                try {
                    if (line != null && line.trim().equalsIgnoreCase("q")) {
                        System.out.println("Exiting");
                        protocolServer.stop();
                        return;
                    } else {
                        System.out.println("Command not recognized: " + line.trim());
                    }
                }catch (Exception ex){
                    System.out.println("Exiting");
                    System.err.println(ex);
                    return;
                }
            }
        }catch (Exception ex){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("runner", options);
        }


    }

    private static void runMongo(int port,String logsDir, String connectionString, String login, String password) {
        var baseProtocol = new MongoProtocol(port);
        var proxy = new MongoProxy(connectionString);
        if (logsDir!=null) {
            proxy.setStorage(new MongoFileStorage(Path.of(logsDir)));
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);
    }

    private static void runPostgres(int port,String logsDir, String connectionString, String login, String password) {
        runJdbc("org.postgresql.Driver",port,logsDir,connectionString,login,password);
    }

    private static void runMysql(int port,String logsDir, String connectionString, String login, String password) {
        runJdbc("com.mysql.cj.jdbc.Driver",port,logsDir,connectionString,login,password);
    }

    private static void runJdbc(String driver, int port, String logsDir, String connectionString, String login, String password) {
        var baseProtocol = new PostgresProtocol(port);
        var proxy = new JdbcProxy(driver,
                connectionString,
                login, password);
        if (logsDir!=null) {
            proxy.setStorage(new JdbcFileStorage(Path.of(logsDir)));
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);
    }
}