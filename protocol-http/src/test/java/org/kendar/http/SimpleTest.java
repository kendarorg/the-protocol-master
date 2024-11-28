package org.kendar.http;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kendar.http.plugins.HttpLatencyPlugin;
import org.kendar.http.plugins.HttpLatencyPluginSettings;
import org.kendar.http.plugins.HttpRateLimitPlugin;
import org.kendar.http.plugins.HttpRateLimitPluginSettings;
import org.kendar.http.utils.ConsumeException;
import org.kendar.http.utils.NullEntity;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.Sleeper;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleTest extends BasicTest {

    private static byte[] buildBytesData() {
        try {
            var frf = new FileResourcesUtils();
            return frf.getFileFromResourceAsByteArray("resource://image.gif");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ByteArrayEntity buildByteEntity() {
        try {


            var entity = new ByteArrayEntity(buildBytesData());
            return entity;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


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


    private static byte[] getConfusingByteArray() {
        return new byte[]{1, 2, 3, 4, 5, '\r', '\n'};
    }

    private static Stream<Arguments> provideTestSituations() {
        try {
            return Stream.of(
                    Arguments.of(
                            "DSC:jsonizedMultipartForm",
                            MultipartEntityBuilder
                                    .create()
                                    .addTextBody("number", "5555555555")
                                    .addTextBody("clip", "rickroll")
                                    .addBinaryBody("upload_file", getConfusingByteArray(),
                                            ContentType.IMAGE_JPEG, "filename")
                                    .addTextBody("tos", "agree")
                                    .build(),
                            new HttpPost("http://localhost:" + 8456 + "/jsonized"),
                            Map.of(),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains("5555555555"));
                                assertTrue(content.contains(new String(Base64.encode(getConfusingByteArray()))));
                                assertTrue(content.contains("agree"));
                                assertTrue(content.contains("tos"));
                                assertTrue(content.contains("clip"));
                            }),
                    Arguments.of(
                            "DSC:jsonizedPostJsonString",
                            new StringEntity("{\"id\":1,\"name\":\"John\"}"),
                            new HttpPost("http://localhost:" + 8456 + "/jsonized"),
                            Map.of("Content-Type", "application/json"),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains("\"John\""));
                            }),
                    Arguments.of(
                            "DSC:straightRetrieveGif",
                            new NullEntity(),
                            new HttpGet("http://localhost:" + 8456 + "/image.gif"),
                            Map.of(),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var baos = new ByteArrayOutputStream();
                                httpResponse.getEntity().writeTo(baos);
                                var actual = baos.toByteArray();
                                assertArrayEquals(buildBytesData(), actual);
                            }),
                    Arguments.of(
                            "DSC:jsonizedPostBinaryContent",
                            buildByteEntity(),
                            new HttpPost("http://localhost:" + 8456 + "/jsonized"),
                            Map.of("Content-Type", "image/gif"),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var binaryNode = new BinaryNode(buildBytesData());
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains(binaryNode.toString()));
                            }),
                    Arguments.of(
                            "DSC:JsonizedPostBinaryContentConsideredText",
                            buildByteEntity(),
                            new HttpPost("http://localhost:" + 8456 + "/jsonized"),
                            Map.of(),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains("GIF89a"));
                            }),
                    Arguments.of(
                            "DSC:jsonizeTestQueryParameters",
                            new NullEntity(),
                            withQuery(new HttpGet("http://localhost:" + 8456 + "/jsonized"), Map.of("par1", "par1Value")),
                            Map.of(),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains("par1Value"));
                            })
                    , Arguments.of(
                            "DSC:jsonizedPostJsonStringGzip",
                            new GzipCompressingEntity(new StringEntity("{\"id\":1,\"name\":\"John\"}")),
                            new HttpPost("http://localhost:" + 8456 + "/jsonized"),
                            //new HttpPost("https://echo.free.beeceptor.com"),
                            Map.of("Content-Type", "application/json"),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains("\"John\""));
                            })
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    void testExternalProxy() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("record-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("https://www.google.com");
        var httpresponse = httpclient.execute(httpget);
        var sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        var content = "";
        while (sc.hasNext()) {
            content += sc.nextLine();
        }
        assertTrue(content.toLowerCase().contains("<title>google</title>"));
    }

    @Test
    void testLatencyPlugin() throws Exception {
        var latencyPlugin = (HttpLatencyPlugin) baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("latency-plugin")).findFirst().get();
        var lps = new HttpLatencyPluginSettings();
        lps.setMinMs(2000);
        lps.setMaxMs(3000);
        latencyPlugin.initialize(globalSettings, httpProtocolSettings, lps);
        latencyPlugin.setActive(true);

        var cf = new ChangeableReference<>("");
        var realTime = new ChangeableReference<>(0L);
        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("https://www.google.com");
        new Thread(() -> {
            try {
                Instant start_time = Instant.now();
                var httpresponse = httpclient.execute(httpget);
                var sc = new Scanner(httpresponse.getEntity().getContent());
                Instant stop_time = Instant.now();
                realTime.set(Duration.between(start_time, stop_time).toMillis());
                //Printing the status line
                assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
                var content = "";
                while (sc.hasNext()) {
                    content += sc.nextLine();
                }
                cf.set(content);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
        Sleeper.sleep(4000);
        assertTrue(realTime.get() >= 2000);
        assertTrue(cf.get().toLowerCase().contains("<title>google</title>"));
    }

    @ParameterizedTest
    @MethodSource("provideTestSituations")
    void allSamplesTest(String description, HttpEntity entity, HttpRequestBase httpPost, Map<String, String> headers, ConsumeException<HttpResponse> consumer) throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("record-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var httpclient = createHttpsHttpClient();

        if (HttpEntityEnclosingRequestBase.class.isAssignableFrom(httpPost.getClass())) {
            ((HttpEntityEnclosingRequestBase) httpPost).setEntity(entity);
        }
        for (var h : headers.entrySet()) {
            httpPost.setHeader(h.getKey(), h.getValue());
        }
        var httpresponse = httpclient.execute(httpPost);
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        consumer.accept(httpresponse);
    }

    @Test
    void testRateLimit() throws Exception {
        var latencyPlugin = (HttpRateLimitPlugin) baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("rate-limit-plugin")).findFirst().get();
        var lps = new HttpRateLimitPluginSettings();
        lps.setResetTimeWindowSeconds(3);
        latencyPlugin.initialize(globalSettings, httpProtocolSettings, lps);
        latencyPlugin.setActive(true);

        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("http://localhost:" + 8456 + "/clean");

        var startWarning = (((double) lps.getRateLimit() / 100) * (double) lps.getWarningThresholdPercent()) / (double) lps.getCostPerRequest();
        var error = lps.getRateLimit() / lps.getCostPerRequest();
        for (var i = 0; i < 100; i++) {
            var httpresponse = httpclient.execute(httpget);
            var sl = httpresponse.getStatusLine().toString().trim();
            if (i > error) {
                assertEquals("HTTP/1.1 200 OK", sl);
                assertNull(httpresponse.getFirstHeader("RateLimit-Limit"));
            } else if (i == error) {
                assertEquals("HTTP/1.1 429", sl);
                var retryAfter = Integer.parseInt(httpresponse.getFirstHeader("Retry-After").getValue());
                assertTrue(retryAfter >= 0 && retryAfter <= 3);
                Sleeper.sleep((retryAfter + 1) * 1000);
            } else {
                assertEquals("HTTP/1.1 200 OK", sl);
                if (i < startWarning) {
                    assertNull(httpresponse.getFirstHeader("RateLimit-Limit"));
                } else {
                    assertEquals(httpresponse.getFirstHeader("RateLimit-Limit").getValue(), "120");
                    var limit = lps.getRateLimit() - ((i + 1) * lps.getCostPerRequest());
                    assertEquals(httpresponse.getFirstHeader("RateLimit-Remaining").getValue(), "" + limit);
                }
            }
            var sc = new Scanner(httpresponse.getEntity().getContent());
            while (sc.hasNext()) {
                sc.nextLine();
            }
        }
    }


    @Test
    void testRateLimitCustom() throws Exception {
        var latencyPlugin = (HttpRateLimitPlugin) baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("rate-limit-plugin")).findFirst().get();
        var lps = new HttpRateLimitPluginSettings();
        lps.setResetTimeWindowSeconds(3);
        lps.setCustomResponseFile(Path.of("src", "test", "resources", "ratelimitresponse.json").toString());
        latencyPlugin.initialize(globalSettings, httpProtocolSettings, lps);
        latencyPlugin.setActive(true);

        var httpclient = createHttpsHttpClient();
        var httpget = new HttpGet("http://localhost:" + 8456 + "/clean");

        var startWarning = (((double) lps.getRateLimit() / 100) * (double) lps.getWarningThresholdPercent()) / (double) lps.getCostPerRequest();
        var error = lps.getRateLimit() / lps.getCostPerRequest();
        for (var i = 0; i < 100; i++) {
            var httpresponse = httpclient.execute(httpget);
            var sl = httpresponse.getStatusLine().toString().trim();
            var sc = new Scanner(httpresponse.getEntity().getContent());
            var cnt = "";
            while (sc.hasNext()) {
                cnt += sc.nextLine();
            }
            if (i > error) {
                assertEquals("HTTP/1.1 200 OK", sl);
                assertNull(httpresponse.getFirstHeader("RateLimit-Limit"));
            } else if (i == error) {
                assertEquals("HTTP/1.1 403 Forbidden", sl);
                var retryAfter = Integer.parseInt(httpresponse.getFirstHeader("Retry-After").getValue());
                assertTrue(retryAfter >= 0 && retryAfter <= 3);
                assertTrue(cnt.contains("You have exceeded a secondary rate limit"));
                Sleeper.sleep((retryAfter + 1) * 1000);
            } else {
                assertEquals("HTTP/1.1 200 OK", sl);
                if (i < startWarning) {
                    assertNull(httpresponse.getFirstHeader("RateLimit-Limit"));
                } else {
                    assertEquals(httpresponse.getFirstHeader("RateLimit-Limit").getValue(), "120");
                    var limit = lps.getRateLimit() - ((i + 1) * lps.getCostPerRequest());
                    assertEquals(httpresponse.getFirstHeader("RateLimit-Remaining").getValue(), "" + limit);
                }
            }

        }
    }

}
