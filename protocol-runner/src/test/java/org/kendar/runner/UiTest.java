package org.kendar.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.plugins.apis.Ok;
import org.kendar.utils.Sleeper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

                "-cfg", Path.of("src", "test", "resources", "uitest.json").toString(),"-unattended"
        };
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
    }

    @Test
    void httpApiTest() throws Exception {

        Sleeper.sleep(1000);
        var httpclient = HttpClients.createDefault();
        var actual = downloadRequestString("http://localhost:5005", httpclient);
        assertTrue(actual.contains("target/tests/uitest"));

        actual = downloadRequestString("http://localhost:5005/plugins", httpclient);
        assertTrue(actual.contains("http-01"));
        assertTrue(actual.contains("redis-01"));
        assertTrue(actual.contains("mysql-01"));
        assertTrue(actual.contains("mqtt-01"));
        assertTrue(actual.contains("postgres-01"));
        assertTrue(actual.contains("amqp091-01"));
        assertTrue(actual.contains("mongodb-01"));
        actual = downloadRequestString("http://localhost:5005/plugins/http-01/ssl-plugin", httpclient);
        assertTrue(actual.contains("C=US,O=Local Development,CN=local.org"));
        actual = downloadRequestString("http://localhost:5005/globalpl", httpclient);
        assertTrue(actual.contains("report-plugin"));
        actual = downloadRequestString("http://localhost:5005/plugins/global/report-plugin", httpclient);
        assertTrue(actual.contains("Report data browser"));

        var data = Files.readAllBytes(Path.of("src", "test", "resources", "testcontent.zip"));
        var okResult = postRequest("http://localhost:5005/api/global/storage", httpclient, data, new TypeReference<Ok>() {
        }, "application/zip");
        assertEquals("OK", okResult.getResult());
        //Sleeper.sleep(50000000);
        actual = downloadRequestString("http://localhost:5005/storage", httpclient);
        assertTrue(actual.contains("/storage/files?parent"));
        actual = downloadRequestString("http://localhost:5005/storage/tree?parent=", httpclient);
        assertTrue(actual.contains("scenario"));
        actual = downloadRequestString("http://localhost:5005/storage/files?parent=scenario", httpclient);
        assertTrue(actual.contains("0000000001.default"));
        actual = downloadRequestString("http://localhost:5005/storage/file?parent=scenario/0000000001.default&name=0000000001.default", httpclient);
        assertTrue(actual.contains("\"fixedHeader\": \"CONNACK\""));
        actual = downloadRequestString("http://localhost:5005/plugins/global/report-plugin", httpclient);
        assertTrue(actual.contains("Global/report-plugin"));



        var tpmql=URLEncoder.encode("SELECT(WHAT(date=MSTODATE(timestamp),instanceId,protocol,query=SUBSTR(query,50),duration,tags=WRAP(tags,50,' ')),ORDERBY(DESC(date)) )");
        var path = "http://localhost:5005/api/global/plugins/report-plugin/report?"+
                "tpmql="+tpmql+
                "&start=0&limit=10";
        actual = new String(downloadRequest(path+"&format=json", httpclient), StandardCharsets.UTF_8);
        //var msg =actual;
        assertTrue(actual.contains("2025/03/13 10:11:22.190"));
        actual = new String(downloadRequest(path+"&format=csv", httpclient), StandardCharsets.UTF_8);
        assertTrue(actual.contains("0,\"2025/03/13 10:11:22.190\","));
        actual = new String(downloadRequest(path+"&format=html", httpclient), StandardCharsets.UTF_8);
        assertTrue(actual.contains("<td>2025/03/13 10:11:22.190</td>"));



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
