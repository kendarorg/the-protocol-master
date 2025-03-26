package org.kendar.runner;

public class UiSeleniumTest extends SeleniumTestBase{
    /*private static BasicTest bs;

    @AfterAll
    public static void cleanup() {
        Main.stop();
        tearDownAfterClassBase();
    }

    @BeforeAll
    public static void setup() throws Exception {
        setupDirectories("seleniumtest");
        try {
            Main.stop();
        } catch (Exception e) {
        }
        var args = new String[]{

                "-cfg", Path.of("src", "test", "resources", "selenium_settings.json").toString()
        };
        var sourceData = Path.of("src", "test", "resources", "data");
        var destData = Path.of("target", "data", "selenium");
        FileUtils.copyDirectory(sourceData.toFile(), destData.toFile());
        sourceData = Path.of("..","target", "plugins");
        destData = Path.of("target", "data", "plugins");
        FileUtils.copyDirectory(sourceData.toFile(), destData.toFile());
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
    }

    @AfterEach
    public void tearDownAfterEach() throws Exception {
        writeScenario();
        tearDownAfterEachBase();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) throws Exception {
        beforeEachBase(testInfo,9999);
    }

    //RUN_VISIBLE=true
    @Test
    public void basicTest() throws Exception {
        navigateTo("http://localhost:8095/", false);
        Sleeper.sleep(1000);
        navigateTo("http://localhost:8095/storage", false);
        Sleeper.sleep(1000);
        navigateTo("http://localhost:8095/recording", false);
        Sleeper.sleep(1000);
        navigateTo("http://localhost:8095/protocols", false);
        Sleeper.sleep(1000);
        navigateTo("http://localhost:8095/plugins", false);
        Sleeper.sleep(1000);
        navigateTo("http://localhost:8095/globalpl", false);
        Sleeper.sleep(1000);
        navigateTo("http://localhost:8095/swagger-ui/index.html#/", false);
        Sleeper.sleep(1000);
    }*/
}
