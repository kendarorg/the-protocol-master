package org.kendar.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ParserTest {
    @Test
    void testDuplicatesOptionsLevel() {
        var options = CommandOptions.of("main");
        var res = assertThrows(RuntimeException.class, () -> options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage")));
        assertEquals("Duplicate inherited command s on option CommandOption{shortCommand='s',longCommand='storage'}", res.getMessage());
    }

    @Test
    void testDuplicatesOptionsLevelShort() {
        var options = CommandOptions.of("main");
        var res = assertThrows(RuntimeException.class, () -> options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("xxx")));
        assertEquals("Duplicate inherited command s on option CommandOption{shortCommand='s',longCommand='xxx'}", res.getMessage());
    }

    @Test
    void testDuplicatesSubChoiceOptionLevel() {
        var options = CommandOptions.of("main");
        var res = assertThrows(RuntimeException.class, () -> options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        ),
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        )
                        )));
        assertEquals("Duplicate sub choice http", res.getMessage());
    }


    @Test
    void testDuplicatesOptionOptionLevel() {
        var options = CommandOptions.of("main");
        var res = assertThrows(RuntimeException.class, () -> options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("p", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        )
                        )));
        assertEquals("Duplicate inherited command p on option CommandOption{shortCommand='p',longCommand='protocol'}", res.getMessage());
    }

    @Test
    void test() {
        var args = new String[]{"-s", "dir", "-p", "http", "-hp", "8080"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        )
                        ));
        var parser = new CommandParser(options);
        parser.parse(args);
        System.out.println(parser.getMainArgs());
    }

    @Test
    void testMandatoryParameter() {
        var args = new String[]{"-s", "dir", "-a"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withLong("storage"),
                CommandOption.of("a", "ApiPort")
                        .withMandatoryParameter()
                        .withLong("xxx"));

        var parser = new CommandParser(options);
        var res = assertThrows(RuntimeException.class, () -> parser.parse(args));
        assertEquals("Mandatory parameter CommandOption{shortCommand='a',longCommand='xxx'} not present", res.getMessage());
    }

    @Test
    void testMissingSubChoice() {
        var args = new String[]{"-s", "dir", "-p", "missing"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        )
                        ));
        var parser = new CommandParser(options);
        var res = assertThrows(RuntimeException.class, () -> parser.parse(args));
        assertEquals("Wrong value missing in command option " +
                "CommandOption{shortCommand='p',longCommand='protocol'} available choices are http, postgres", res.getMessage());
    }

    @Test
    void testWrongSubChoiceChild() {
        var args = new String[]{"-s", "dir", "-p", "http", "-wrong"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        )
                        ));
        var parser = new CommandParser(options);
        var res = assertThrows(RuntimeException.class, () -> parser.parse(args));
        assertEquals("Unknown options [MainArg{id='wrong', values=[]}]", res.getMessage());
    }


    @Test
    void testWrongThingChoice() {
        var args = new String[]{"-s", "dir", "-p", "http", "-hp", "2035", "-p", "postgres", "-pp", "3207", "-h", "tt"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("pp", "Port")
                                        )
                        ).withMultipleSubChoices());
        var parser = new CommandParser(options);
        var res = assertThrows(RuntimeException.class, () -> parser.parse(args));
        assertEquals("Unknown options [MainArg{id='h', values=[tt]}]", res.getMessage());
    }

    @Test
    void testGoodChoice() {
        var args = new String[]{"-s", "dir", "-p", "http", "-hp", "2035", "-p", "postgres", "-hp", "3207"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                        ),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("hp", "Port")
                                        )
                        ).withMultipleSubChoices());
        var parser = new CommandParser(options);
        parser.parse(args);
        assertTrue(parser.hasOption("storage"));
        assertEquals("dir", parser.getOptionValue("storage"));
        assertTrue(parser.hasOption("p"));
        assertArrayEquals(new String[]{"http", "postgres"}, parser.getOptionValues("protocol").toArray(new String[]{}));
        assertTrue(parser.hasOption("p.http"));
        assertEquals("default", parser.getOptionValue("p.http", "default"));
        assertTrue(parser.hasOption("p.http.hp"));
        assertFalse(parser.hasOption("p.http.pp"));
        assertEquals("2035", parser.getOptionValue("p.http.hp", "default"));
        assertNull(parser.getOptionValue("p.http.ff"));
        assertTrue(parser.hasOption("p.postgres.hp"));
        assertEquals("3207", parser.getOptionValue("p.postgres.hp", "default"));
    }


    @Test
    void testGetSubs() {
        var args = new String[]{"-s", "dir", "-p", "http", "-hp", "2035", "-p", "postgres", "-hp", "3207"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(

                        ).withMultipleSubChoices());
        var parser = new CommandParser(options);
        parser.parseIgnoreMissing(args);
        CommandOption co = options.getCommandOption("p");
        co.withSubChoices(CommandOptions.of("http")
                        .withOptions(
                                CommandOption.of("hp", "Http Port")
                        ),
                CommandOptions.of("postgres")
                        .withOptions(
                                CommandOption.of("hp", "Port")
                        ));

        parser.parse(args);
        assertTrue(parser.hasOption("storage"));
        assertEquals("dir", parser.getOptionValue("storage"));
        assertTrue(parser.hasOption("p"));
        assertArrayEquals(new String[]{"http", "postgres"}, parser.getOptionValues("protocol").toArray(new String[]{}));
        assertTrue(parser.hasOption("p.http"));
        assertEquals("default", parser.getOptionValue("p.http", "default"));
        assertTrue(parser.hasOption("p.http.hp"));
        assertFalse(parser.hasOption("p.http.pp"));
        assertEquals("2035", parser.getOptionValue("p.http.hp", "default"));
        assertNull(parser.getOptionValue("p.http.ff"));
        assertTrue(parser.hasOption("p.postgres.hp"));
        assertEquals("3207", parser.getOptionValue("p.postgres.hp", "default"));
    }

    @Test
    void testAutoFill() {
        var args = new String[]{"-s", "dir", "-p", "postgres", "-hp", "3207", "-p", "http", "-hp", "2035", "-activate"};
        var options = CommandOptions.of("main");
        var globalSettings = new GlobalSettings();
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage")
                        .withCallback(globalSettings::setStorageDir),
                CommandOption.of("p", "The protocol")
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                                        .withMandatoryParameter()
                                                        .withCallback((s) -> ((HttpSettings) globalSettings.getProtocols().get("http")).setPort(Integer.parseInt(s))),
                                                CommandOption.of("activate", "Activate http")
                                                        .withCallback((s) -> ((HttpSettings) globalSettings.getProtocols().get("http")).setActive(true))
                                        )
                                        .withCallback(s -> globalSettings.getProtocols().put(s, new HttpSettings())),
                                CommandOptions.of("postgres")
                                        .withOptions(
                                                CommandOption.of("hp", "Port")
                                                        .withMandatoryParameter()
                                                        .withCallback((s) -> ((PostgresSettings) globalSettings.getProtocols().get("postgres")).setPort(Integer.parseInt(s)))
                                        )
                                        .withCallback(s -> globalSettings.getProtocols().put(s, new PostgresSettings()))
                        )
                        .withMultipleSubChoices());
        var parser = new CommandParser(options);
        parser.parse(args);
        assertEquals("dir", globalSettings.getStorageDir());
        assertEquals(2, globalSettings.getProtocols().size());
        assertEquals(3207, ((PostgresSettings) globalSettings.getProtocols().get("postgres")).getPort());
        assertEquals(2035, ((HttpSettings) globalSettings.getProtocols().get("http")).getPort());
        assertTrue(((HttpSettings) globalSettings.getProtocols().get("http")).isActive());
    }

    @Test
    void printHelp() {
        var args = new String[]{"-m", "a", "-m", "b", "-protocol", "http"};
        var options = CommandOptions.of("main", "The Protocol Master\ndo all!");
        var globalSettings = new GlobalSettings();
        options.withOptions(
                CommandOption.of("h", "Help")
                        .withLong("help")
                        .withParameter(),
                CommandOption.of("s", "Storage Dir")
                        .withMandatoryParameter()
                        .withLong("storage")
                        .withCallback(globalSettings::setStorageDir),
                CommandOption.of("m", "Multiple")
                        .withMandatoryParameter()
                        .asMultiple()
                        .withLong("multiple")
                        .withMultiCallback(globalSettings::setMultiple),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http")
                                        .withDescription("Http protocol")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port")
                                                        .withCallback((s) -> ((HttpSettings) globalSettings.getProtocols().get("http")).setPort(Integer.parseInt(s)))
                                        )
                                        .withCallback(s -> globalSettings.getProtocols().put(s, new HttpSettings())),
                                CommandOptions.of("postgres")
                                        .withDescription("Postgres protocol")
                                        .withOptions(
                                                CommandOption.of("hp", "Port")
                                                        .withCallback((s) -> ((PostgresSettings) globalSettings.getProtocols().get("postgres")).setPort(Integer.parseInt(s)))
                                        )
                                        .withCallback(s -> globalSettings.getProtocols().put(s, new PostgresSettings()))
                        )
                        .withMultipleSubChoices());
        var parser = new CommandParser(options);
        var help = parser.buildHelp();
        assertTrue(help.contains("Repeatable"));
        parser.printHelp();
        parser.parse(args);
        assertTrue(parser.hasOption("m"));
        var values = parser.getOptionValues("m");
        assertEquals(2, values.size());
        assertEquals(2, globalSettings.getMultiple().size());
        assertEquals("a", values.get(0));
        assertEquals("b", values.get(1));
    }

    @Test
    void testAutoFillSubCommand() {
        var args = "-a a -c c -d d -e e -b b -c cd".split(" ");
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("a", "ad")
                        .withMandatoryParameter()
                        .withCommandOptions(
                                CommandOption.of("c", "cd")
                                        .withMandatoryParameter()
                                        .withCommandOptions(
                                                CommandOption.of("d", "dd"),
                                                CommandOption.of("e", "ed")
                                        )

                        ),
                CommandOption.of("b", "bd")
                        .withCommandOptions(
                                CommandOption.of("c", "cd")
                                        .withMandatoryParameter()
                        ));
        var parser = new CommandParser(options);
        parser.parse(args);
        assertTrue(parser.hasOption("a"));
        assertEquals("a", parser.getOptionValue("a"));
        assertTrue(parser.hasOption("a.c"));
        assertEquals("c", parser.getOptionValue("a.c"));
        assertTrue(parser.hasOption("a.c.d"));
        assertEquals("d", parser.getOptionValue("a.c.d"));
        assertTrue(parser.hasOption("a.c.e"));
        assertEquals("e", parser.getOptionValue("a.c.e"));

        assertTrue(parser.hasOption("b"));
        assertEquals("b", parser.getOptionValue("b"));
        assertTrue(parser.hasOption("b.c"));
        assertEquals("cd", parser.getOptionValue("b.c"));
    }


    @Test
    void testAutoFillSubCommandNotPresent() {
        var args = "-a a -c c -d d -b b -c cd -e e".split(" ");
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("a", "ad")
                        .withMandatoryParameter()
                        .withCommandOptions(
                                CommandOption.of("c", "cd")
                                        .withMandatoryParameter()
                                        .withCommandOptions(
                                                CommandOption.of("d", "dd"),
                                                CommandOption.of("e", "ed")
                                        )

                        ),
                CommandOption.of("b", "bd")
                        .withCommandOptions(
                                CommandOption.of("c", "cd")
                                        .withMandatoryParameter()
                        ));
        var parser = new CommandParser(options);
        var res = assertThrows(RuntimeException.class, () -> parser.parse(args));
        assertEquals("Unknown options [MainArg{id='e', values=[e]}]", res.getMessage());
    }

    @Test
    void testWrongMat() {
        var args = "-a a -c c -d d -b b -c cd -e e".split(" ");
        var options = CommandOptions.of("main");
        var res = assertThrows(RuntimeException.class, () -> options.withOptions(
                CommandOption.of("a", "ad")
                        .withMandatoryParameter()
                        .withCommandOptions(
                                CommandOption.of("c", "cd")
                                        .withMandatoryParameter()
                                        .withCommandOptions(
                                                CommandOption.of("d", "dd"),
                                                CommandOption.of("e", "ed")
                                        )

                        ),
                CommandOption.of("c", "bd")));
        assertEquals("Duplicate inherited command c on option CommandOption{shortCommand='a'}", res.getMessage());
    }

    @Test
    void testMultiple() {
        var args = new String[]{"-s", "a=b", "-s", "b=c"};
        var options = CommandOptions.of("main");
        options.withOptions(
                CommandOption.of("s", "Storage Dir")
                        .asMultiple()
                        .withLong("storage"));
        var parser = new CommandParser(options);
        parser.parse(args);
        var result = parser.getOptionValues("s");
        assertEquals(2, result.size());
        assertEquals("a=b", result.get(0));
        assertEquals("b=c", result.get(1));
    }

    @Test
    void testMultilevel() {
        //var args = new String[]{"-j", "-p", "http", "-hp", "-plugin","-pluginChoice","-x","asdf","-bbb",|};
        var options = CommandOptions.of("main", "Root item");
        options.withOptions(
                CommandOption.of("j", "Stuff")
                        .withLong("jjjj"),
                CommandOption.of("p", "The protocol")
                        .withParameter()
                        .withMandatoryParameter()
                        .withLong("protocol")
                        .withSubChoices(
                                CommandOptions.of("http", "HTTP")
                                        .withOptions(
                                                CommandOption.of("hp", "Http Port").withLong("hpl"),
                                                CommandOption.of("plugin", "Plugin")
                                                        .withCommandOptions(
                                                                CommandOption.of("pluginChoice", "Plugin Choice"),
                                                                CommandOption.of("otherChoice", "Plugin Choice")
                                                        )
                                        )
                        )
        );
        var parser = new CommandParser(options);
        var result = parser.buildHelp().trim();
        var expected = "Root item\n" +
                "\n" +
                "  j               jjjj      Stuff\n" +
                "  p               protocol  The protocol\n" +
                "                            Options: http\n" +
                "\n" +
                "HTTP (http)\n" +
                "\n" +
                "  hp              hpl       Http Port\n" +
                "  plugin                    Plugin\n" +
                "    pluginChoice            Plugin Choice\n" +
                "    otherChoice             Plugin Choice";
        assertEquals(expected,result);
        System.out.println(result);
    }
}
