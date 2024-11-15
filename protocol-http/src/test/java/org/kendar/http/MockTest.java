package org.kendar.http;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockTest extends BasicTest{
    @BeforeAll
    public static void beforeClass() throws Exception {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() {
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

    private static void runAndFind( CloseableHttpClient httpclient, HttpGet httpget,  String x) throws IOException {

        var httpresponse = httpclient.execute(httpget);
        var content = getContentString(httpresponse);
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        assertTrue(content.toLowerCase().contains(x.toLowerCase()));
    }

    @Test
    void countedMock() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("http://localhost:" + 8456 + "/jsonized");
        httpget = (HttpGet) withQuery(httpget, Map.of("counter","true"));


        runAndFind(httpclient, httpget,"google.ps===void");
        runAndFind(httpclient, httpget,"google.ps===void");
        runAndFind( httpclient, httpget,  "x-block-recursive");
    }

    @Test
    void nthCall() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("http://localhost:" + 8456 + "/jsonized");
        httpget = (HttpGet) withQuery(httpget, Map.of("nth","true"));


        runAndFind( httpclient, httpget,  "x-block-recursive");
        runAndFind( httpclient, httpget,  "google.ps===void");
        runAndFind( httpclient, httpget,  "x-block-recursive");
    }

    @Test
    void countedNthMock() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("http://localhost:" + 8456 + "/jsonized");
        httpget = (HttpGet) withQuery(httpget, Map.of("both","true"));


        runAndFind( httpclient, httpget,  "x-block-recursive");
        runAndFind(httpclient, httpget,"google.ps===void");
        runAndFind(httpclient, httpget,"google.ps===void");
        runAndFind(httpclient, httpget,"google.ps===void");
        runAndFind( httpclient, httpget,  "x-block-recursive");
    }
}
