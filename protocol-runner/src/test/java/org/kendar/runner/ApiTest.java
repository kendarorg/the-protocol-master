package org.kendar.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.apis.dtos.PluginIndex;
import org.kendar.apis.dtos.ProtocolIndex;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.Sleeper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest extends BasicTest {
    private static BasicTest bs;

    @AfterAll
    public static void cleanup() {
        bs.runTheServer.set(false);
        Main.stop();
        Sleeper.sleep(1000);
    }

    @BeforeAll
    public static void setup() {
        try {
            Main.stop();
        } catch (Exception e) {
        }
        Sleeper.sleep(1000);
        var args = new String[]{

                "-cfg", Path.of("src", "test", "resources", "apitest.json").toString()
        };
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
        Sleeper.sleep(2000);
    }

    private static <T> T getRequest(String target, CloseableHttpClient httpclient, TypeReference<T> typeReference) throws IOException {
        var httpget = new HttpGet(target);
        var httpresponse = httpclient.execute(httpget);

        var sc = new Scanner(httpresponse.getEntity().getContent());
        var result = "";
        while (sc.hasNext()) {
            result += (sc.nextLine());
        }
        System.out.println(result);
        return mapper.deserialize(result, typeReference);
    }

    private static byte[] downloadRequest(String target, CloseableHttpClient httpclient) throws IOException {
        var httpget = new HttpGet(target);
        var httpresponse = httpclient.execute(httpget);

        var baos = new ByteArrayOutputStream();
        httpresponse.getEntity().writeTo(baos);
        return baos.toByteArray();
    }

    private static String downloadRequestString(String target, CloseableHttpClient httpclient) throws IOException {
        var bytes = downloadRequest(target, httpclient);
        return new String(bytes);
    }

    private static <T> T postRequest(String target, CloseableHttpClient httpclient, byte[] data, TypeReference<T> typeReference) throws IOException {
        var httpget = new HttpPost(target);
        var be = new ByteArrayEntity(data);
        httpget.setEntity(be);
        var httpresponse = httpclient.execute(httpget);

        var sc = new Scanner(httpresponse.getEntity().getContent());
        var result = "";
        while (sc.hasNext()) {
            result += (sc.nextLine());
        }
        return mapper.deserialize(result, typeReference);
    }

    @Test
    void globalApiTest() throws Exception {

        var httpclient = HttpClients.createDefault();
        var data = Files.readAllBytes(Path.of("src", "test", "resources", "testcontent.zip"));
        var okResult = postRequest("http://localhost:5005/api/global/storage", httpclient, data, new TypeReference<Ok>() {
        });
        assertEquals("OK", okResult.getResult());
        var zip = downloadRequest("http://localhost:5005/api/global/storage", httpclient);
        assertTrue(zip.length > 100);
        Files.write(Path.of("target", "downloaded.zip"), zip);

    }

    @Test
    void httpApiTest() throws Exception {
        var frsu = new FileResourcesUtils();

        Sleeper.sleep(1000);
        var httpclient = HttpClients.createDefault();
        var expected = frsu.getFileFromResourceAsByteArray("resource://certificates/ca.der");
        var actual = downloadRequest("http://localhost:5005/api/protocols/http-01/plugins/ssl-plugin/der", httpclient);
        assertArrayEquals(expected, actual);

        expected = frsu.getFileFromResourceAsByteArray("resource://certificates/ca.key");
        actual = downloadRequest("http://localhost:5005/api/protocols/http-01/plugins/ssl-plugin/key", httpclient);
        assertArrayEquals(expected, actual);

    }

    @Test
    void testingStatic() throws Exception {
        var frsu = new FileResourcesUtils();

        Sleeper.sleep(1000);
        var httpclient = HttpClients.createDefault();
        var actual = new String(downloadRequest("http://localhost:5005/swagger/index.html", httpclient));
        assertTrue(actual.contains("swagger-ui"));

        actual = new String(downloadRequest("http://localhost:5005/api/swagger/map.json", httpclient));
        assertTrue(actual.contains("/api/global/terminate"));
        var exp = new FileResourcesUtils().getFileFromResourceAsByteArray("resource://web/swagger/favicon-32x32.png");
        var bin = downloadRequest("http://localhost:5005/swagger/favicon-32x32.png", httpclient);
        assertArrayEquals(exp, bin);
    }

    @Test
    void protocolApiTest() throws Exception {

        var httpclient = HttpClients.createDefault();
        //Creating a HttpGet object
        var protocols = getRequest("http://localhost:5005/api/protocols", httpclient, new TypeReference<List<ProtocolIndex>>() {
        });
        assertEquals(7, protocols.size());

        var settings = downloadRequestString("http://localhost:5005/api/global/settings", httpclient);
        assertTrue(settings.contains("target/tests/apitest"));
        assertTrue(settings.contains("5005"));

        var plugins = getRequest("http://localhost:5005/api/protocols/redis-01/plugins", httpclient, new TypeReference<List<PluginIndex>>() {
        });
        assertEquals(2, plugins.size());
        assertTrue(plugins.stream().allMatch(p -> !p.isActive()));


        var okResult = getRequest("http://localhost:5005/api/protocols/all/plugins/record-plugin/start", httpclient, new TypeReference<Ok>() {
        });
        assertEquals("OK", okResult.getResult());
        for (var protocol : protocols) {
            var status = getRequest("http://localhost:5005/api/protocols/" + protocol.getId() + "/plugins/record-plugin/status", httpclient, new TypeReference<Status>() {
            });
            assertTrue(status.isActive());
        }
        for (var protocol : protocols) {
            okResult = getRequest("http://localhost:5005/api/protocols/" + protocol.getId() + "/plugins/record-plugin/stop", httpclient, new TypeReference<Ok>() {
            });
            assertEquals("OK", okResult.getResult());
        }
        for (var protocol : protocols) {
            var status = getRequest("http://localhost:5005/api/protocols/" + protocol.getId() + "/plugins/record-plugin/start", httpclient, new TypeReference<Status>() {
            });
            assertFalse(status.isActive());
        }

    }
}
