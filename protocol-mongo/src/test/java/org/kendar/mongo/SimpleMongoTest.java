package org.kendar.mongo;

import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SimpleMongoTest extends BasicTest {
    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void testConnection() throws SQLException, ClassNotFoundException {
        var c = getProxyConnection();
        var db = c.getDatabase("admin");
        Bson command = new BsonDocument("dbStats", new BsonInt64(1));
        Document commandResult = db.runCommand(command);
        assertNotNull(commandResult);
        System.out.println(commandResult.toJson());
        var ol = db.getCollection("testcollection");
        var result = ol.insertOne(new Document()
                .append("_id", new ObjectId())
                .append("title", "Ski Bloopers")
                .append("genres", Arrays.asList("Documentary", "Comedy")));
        Bson filter = Filters.eq("_id", result.getInsertedId());
        Document doc = ol.find(filter).first();
        System.out.println(doc.toJson());
        assertEquals("Ski Bloopers", doc.get("title").toString());
        assertNotNull(doc);
        assertNotNull(c);

        var events =getEvents().stream().collect(Collectors.toList());
        assertEquals(3,events.size());
        var evt= events.get(0);
        assertEquals("mongodb",evt.getProtocol());
        assertEquals(1,events.stream().filter(e->e.getQuery().contains(":dbStats")).count());
        assertEquals(1,events.stream().filter(e->e.getQuery().contains(":insert")).count());
        assertEquals(1,events.stream().filter(e->e.getQuery().contains(":find")).count());
    }

    @Test
    void testConnectionAuth() throws SQLException, ClassNotFoundException {
        var c = getProxyConnectionAuth();
        var db = c.getDatabase("admin");
        Bson command = new BsonDocument("dbStats", new BsonInt64(1));
        Document commandResult = db.runCommand(command);
        assertNotNull(commandResult);
        System.out.println(commandResult.toJson());
        var ol = db.getCollection("testcollection");
        var result = ol.insertOne(new Document()
                .append("_id", new ObjectId())
                .append("title", "Ski Bloopers")
                .append("genres", Arrays.asList("Documentary", "Comedy")));
        Bson filter = Filters.eq("_id", result.getInsertedId());
        Document doc = ol.find(filter).first();
        System.out.println(doc.toJson());
        assertEquals("Ski Bloopers", doc.get("title").toString());
        assertNotNull(doc);
        assertNotNull(c);
    }


    @Test
    void testConnectionWithProtocolWithServerApis() throws SQLException, ClassNotFoundException {
        var c = getProxyConnectionWithServerApis();
        var db = c.getDatabase("admin");
        Bson command = new BsonDocument("dbStats", new BsonInt64(1));
        Document commandResult = db.runCommand(command);
        assertNotNull(commandResult);
        System.out.println(commandResult.toJson());
        var ol = db.getCollection("testcollection");
        var result = ol.insertOne(new Document()
                .append("_id", new ObjectId())
                .append("title", "Ski Bloopers")
                .append("genres", Arrays.asList("Documentary", "Comedy")));
        Bson filter = Filters.eq("_id", result.getInsertedId());
        Document doc = ol.find(filter).first();
        System.out.println(doc.toJson());
        assertEquals("Ski Bloopers", doc.get("title").toString());
        assertNotNull(doc);
        assertNotNull(c);
    }
}
