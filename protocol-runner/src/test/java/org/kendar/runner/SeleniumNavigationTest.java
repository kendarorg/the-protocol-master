package org.kendar.runner;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;

import java.io.IOException;
import java.nio.file.Path;

public class SeleniumNavigationTest extends ApiTestBase {
    private static BasicTest bs;

    @AfterAll
    public static void cleanup() {
        Main.stop();

    }

    @BeforeAll
    public static void setup() throws IOException {
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


    @Test
    void test() throws Exception {

        System.out.println("Running Selenium Test");
    }

}
