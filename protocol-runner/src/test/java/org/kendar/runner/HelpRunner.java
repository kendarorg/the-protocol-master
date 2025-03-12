package org.kendar.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.utils.Sleeper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelpRunner {
    private ByteArrayOutputStream myOut;
    private PrintStream oriOut;

    @BeforeEach
    void beforeEach() {
        myOut = new ByteArrayOutputStream();
        oriOut = System.out;
        System.setOut(new PrintStream(myOut));
    }

    private String getOut() {
        var returning = myOut.toString();
        System.setOut(oriOut);
        return returning;
    }

    @Test
    void testHelp() throws Exception {
        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help","-unattended"
        };

        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
    }

    @Test
    void testHttpHelp() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "http","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        ;
        assertTrue(standardOutput.contains("http"));
    }

    @Test
    void testMysqlHelp() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "mysql","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        ;
        assertTrue(standardOutput.contains("mysql"));
    }

    @Test
    void testPostgresHelp() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "postgres","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        ;
        assertTrue(standardOutput.contains("postgres"));
    }

    @Test
    void testAmqp091Help() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "amqp091","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        assertTrue(standardOutput.contains("amqp091"));
    }


    @Test
    void testMqttHelp() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "mqtt","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        assertTrue(standardOutput.contains("mqtt"));
    }

    @Test
    void testRedisHelp() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "redis","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        assertTrue(standardOutput.contains("redis"));
    }

    @Test
    void testMongoHelp() throws Exception {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir",Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-help", "mongodb","-unattended"
        };
        Main.execute(args);
        final String standardOutput = getOut();
        System.out.println(standardOutput);
        ;
        assertTrue(standardOutput.contains("mongodb"));
    }
}
