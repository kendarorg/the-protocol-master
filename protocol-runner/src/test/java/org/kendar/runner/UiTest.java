package org.kendar.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.plugins.apis.Ok;

import java.nio.file.Files;
import java.nio.file.Path;

public class UiTest extends ApiTestBase {
    private static BasicTest bs;

    @AfterAll
    public static void cleanup() {
        Main.stop();
    }

    @BeforeAll
    public static void setup() {
        try {
            Main.stop();
        } catch (Exception e) {
        }
        var args = new String[]{

                "-cfg", Path.of("src", "test", "resources", "apitestsencstorage.json").toString()
        };
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
    }

//    @Test
//    void globalApiTest() throws Exception {
//
//        var httpclient = HttpClients.createDefault();
//        var data = Files.readAllBytes(Path.of("src", "test", "resources", "testcontent_enc.zip"));
//        var okResult = postRequest("http://localhost:5005/api/global/storage", httpclient, data, new TypeReference<Ok>() {
//        }, "application/zip");
//    }
}
