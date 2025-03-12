package org.kendar.runner;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.jupiter.api.*;
import org.kendar.Main;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.GlobalReport;
import org.kendar.runner.utils.SimpleHttpServer;
import org.kendar.tests.jpa.HibernateSessionFactory;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiRecordReplayTest extends BasicTest {

    private static SimpleHttpServer simpleServer;
    private static String POSTGRES_PORT = "5631";
    private static String HTTP_PORT = "12080";
    private static String HTTPS_PORT = "12443";
    private static String PROXY_PORT = "1281";
    private static int SIMPLE_SERVER_HTTP_PORT = 18080;
    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    private AtomicBoolean runTheServer = new AtomicBoolean(true);

    @BeforeAll
    public static void beforeClass() throws IOException {
        simpleServer = new SimpleHttpServer();
        simpleServer.start(SIMPLE_SERVER_HTTP_PORT);
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
        simpleServer.stop();
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        runTheServer.set(true);
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
    }

    @AfterEach
    public void afterEach() {
        EventsQueue.unregister("recorder", ReportDataEvent.class);

        runTheServer.set(false);
        Main.stop();
        Sleeper.sleep(100);
        events.clear();
    }


    @Test
    void testRecordingReplaying() throws Exception {
        var targetDir = Path.of("target", "multiRecording").toFile();
        targetDir.mkdir();
        for (var f : targetDir.listFiles()) f.delete();
        var proxy = new HttpHost("localhost", Integer.parseInt(PROXY_PORT), "http");
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        var httpclient = HttpClients.custom().setRoutePlanner(routePlanner).build();
        var httpget = new HttpGet("http://localhost:" + SIMPLE_SERVER_HTTP_PORT);

        var fr = new FileResourcesUtils();
        var recordingSettings = fr.getFileFromResourceAsString(Path.of("src", "test", "resources", "multiRecording.json.template").toAbsolutePath().toString());
        recordingSettings = recordingSettings.replace("{postgresPort}", POSTGRES_PORT);
        recordingSettings = recordingSettings.replace("{httpPort}", HTTP_PORT);
        recordingSettings = recordingSettings.replace("{httpsPort}", HTTPS_PORT);
        recordingSettings = recordingSettings.replace("{proxyPort}", PROXY_PORT);
        recordingSettings = recordingSettings.replace("{postgresLogin}", postgresContainer.getUserId());
        recordingSettings = recordingSettings.replace("{postgresPassword}", postgresContainer.getPassword());
        recordingSettings = recordingSettings.replace("{postgresConnection}", postgresContainer.getJdbcUrl());
        recordingSettings = recordingSettings.replace("{dataDir}", Path.of("target", "multiRecording").toAbsolutePath().toString().replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\")));
        recordingSettings = recordingSettings.replaceAll(Pattern.quote("{recordActive}"), "true");
        recordingSettings = recordingSettings.replaceAll(Pattern.quote("{replayActive}"), "false");
        var recordingConfig = Path.of("target", "multiRecording", "recording.json").toAbsolutePath();
        Files.writeString(recordingConfig, recordingSettings);

        System.out.println("STARTING ==============================================");
        startAndHandleUnexpectedErrors("-cfg", recordingConfig.toString());
        System.out.println("RECORDING ==============================================");


        HibernateSessionFactory.initialize("org.postgresql.Driver",
                //postgresContainer.getJdbcUrl(),
                String.format("jdbc:postgresql://127.0.0.1:%d/test?ssl=false", Integer.parseInt(POSTGRES_PORT)),
                postgresContainer.getUserId(), postgresContainer.getPassword(),
                "org.hibernate.dialect.PostgreSQLDialect",
                CompanyJpa.class);

        HibernateSessionFactory.transactional(em -> {
            var lt = new CompanyJpa();
            lt.setDenomination("Test Ltd");
            lt.setAddress("TEST RD");
            lt.setAge(22);
            lt.setSalary(500.22);
            em.persist(lt);
        });


        var httpresponse = httpclient.execute(httpget);
        var sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        StringBuilder content = new StringBuilder();
        while (sc.hasNext()) {
            content.append(sc.nextLine());
        }
        assertTrue(content.toString().toLowerCase().contains("X-block-recursive".toLowerCase()));

        var verifyTestRun = new AtomicBoolean(false);
        HibernateSessionFactory.query(em -> {
            var resultset = em.createQuery("SELECT denomination FROM CompanyJpa").getResultList();
            for (var rss : resultset) {
                assertEquals("Test Ltd", rss);
                verifyTestRun.set(true);
            }
        });


        var getReport = new HttpGet("http://localhost:9127/api/global/plugins/report-plugin/download");
        httpresponse = httpclient.execute(getReport);
        sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        content = new StringBuilder();
        while (sc.hasNext()) {
            content.append(sc.nextLine());
        }
        var data = mapper.deserialize(content.toString(), GlobalReport.class);

        assertEquals(14, data.getEvents().stream().filter(e -> e.getProtocol().equalsIgnoreCase("postgres")).count());
        assertEquals(1, data.getEvents().stream().filter(e -> e.getProtocol().equalsIgnoreCase("http")).count());


        runTheServer.set(false);
        Main.stop();
        assertTrue(verifyTestRun.get());


        System.out.println("RECORDING COMPLETED ==============================================");

        var replaySettings = fr.getFileFromResourceAsString(Path.of("src", "test", "resources", "multiRecording.json.template").toAbsolutePath().toString());
        replaySettings = replaySettings.replace("{postgresPort}", POSTGRES_PORT);
        replaySettings = replaySettings.replace("{httpPort}", HTTP_PORT);
        replaySettings = replaySettings.replace("{httpsPort}", HTTPS_PORT);
        replaySettings = replaySettings.replace("{proxyPort}", PROXY_PORT);
        replaySettings = replaySettings.replace("{postgresLogin}", "");
        replaySettings = replaySettings.replace("{postgresPassword}", "");
        replaySettings = replaySettings.replace("{postgresConnection}", "");
        replaySettings = replaySettings.replace("{dataDir}", Path.of("target", "multiRecording").toAbsolutePath().toString().replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\")));
        replaySettings = replaySettings.replaceAll(Pattern.quote("{recordActive}"), "false");
        replaySettings = replaySettings.replaceAll(Pattern.quote("{replayActive}"), "true");
        var replayConfig = Path.of("target", "multiRecording", "replaying.json").toAbsolutePath();
        Files.writeString(replayConfig, replaySettings);


        System.out.println("STARTING ==============================================");
        startAndHandleUnexpectedErrors("-cfg", replayConfig.toString());
        System.out.println("REPLAYING ==============================================");

        HibernateSessionFactory.initialize("org.postgresql.Driver",
                //postgresContainer.getJdbcUrl(),
                String.format("jdbc:postgresql://127.0.0.1:%d/test?ssl=false", Integer.parseInt(POSTGRES_PORT)),
                postgresContainer.getUserId(), postgresContainer.getPassword(),
                "org.hibernate.dialect.PostgreSQLDialect",
                CompanyJpa.class);

        HibernateSessionFactory.transactional(em -> {
            var lt = new CompanyJpa();
            lt.setDenomination("Test Ltd");
            lt.setAddress("TEST RD");
            lt.setAge(22);
            lt.setSalary(500.22);
            em.persist(lt);
        });


        var httpFail = new HttpGet("http://localhost:" + SIMPLE_SERVER_HTTP_PORT + "/notRecorded");
        httpresponse = httpclient.execute(httpFail);
        sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 500 Internal Server Error", httpresponse.getStatusLine().toString());


        httpresponse = httpclient.execute(httpget);
        sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        content = new StringBuilder();
        while (sc.hasNext()) {
            content.append(sc.nextLine());
        }
        assertTrue(content.toString().toLowerCase().contains("X-block-recursive".toLowerCase()));

        var verifyTestRun2 = new AtomicBoolean(false);
        HibernateSessionFactory.query(em -> {
            var resultset = em.createQuery("SELECT denomination FROM CompanyJpa").getResultList();
            for (var rss : resultset) {
                assertEquals("Test Ltd", rss);
                verifyTestRun2.set(true);
            }
        });
        runTheServer.set(false);
        Main.stop();
        assertTrue(verifyTestRun2.get());

        System.out.println("COMPLETED SIMULATION ==============================================");

    }
}
