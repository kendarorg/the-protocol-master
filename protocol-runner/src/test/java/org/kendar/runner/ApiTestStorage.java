package org.kendar.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.apis.dtos.CompactLineApi;
import org.kendar.storage.StorageItem;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiTestStorage extends ApiTestBase {
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

                "-cfg", Path.of("src", "test", "resources", "apiteststorage.json").toString()
        };
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
        Sleeper.sleep(3000);
    }

    @Test
    void globalApiTest() throws Exception {

        var httpclient = HttpClients.createDefault();

        var items = getRequest("http://localhost:5005/api/global/storage/items",
                httpclient, new TypeReference<List<CompactLineApi>>() {
        });
        assertEquals(12, items.size());

        var item = getRequest("http://localhost:5005/api/global/storage/items/default/3",
                httpclient, new TypeReference<StorageItem>() {
                });
        assertEquals("JDBC", item.getCaller());

    }

}
