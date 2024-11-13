package org.kendar.http;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.*;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleTest extends BasicTest {

    public static final String MAIN_QUEUE = "fuffa_queue";
    public static final String DEFAULT_MESSAGE_CONTENT = "zzzz details";

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


    @Test
    void googleTest() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("record-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        final var sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        var proxy = new HttpHost("localhost", FAKE_PORT_PROXY, "http");
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        var httpclient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setConnectionManager(
                        new PoolingHttpClientConnectionManager(
                                RegistryBuilder.<ConnectionSocketFactory>create()
                                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                                        .register("https", new SSLConnectionSocketFactory(sslContext,
                                                NoopHostnameVerifier.INSTANCE))
                                        .build()
                        )).
                setRoutePlanner(routePlanner).build();
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
    void simpleTest() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("record-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var proxy = new HttpHost("localhost", FAKE_PORT_PROXY, "http");
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        var httpclient = HttpClients.custom().setRoutePlanner(routePlanner).build();
        var httpget = new HttpGet("http://localhost:" + 8456);
        var httpresponse = httpclient.execute(httpget);
        var sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        var content = "";
        while (sc.hasNext()) {
            content += sc.nextLine();
        }
        assertTrue(content.contains("\"X-block-recursive\":[\"http://localhost:8456/\"]"));
    }

    @Test
    void simpleTestMultipart() throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("record-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var proxy = new HttpHost("localhost", FAKE_PORT_PROXY, "http");
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        var httpclient = HttpClients.custom().setRoutePlanner(routePlanner).build();

        var binaryData = new byte[]{1, 2, 3, 4, 5, '\r', '\n'};
        var entity = MultipartEntityBuilder
                .create()
                .addTextBody("number", "5555555555")
                .addTextBody("clip", "rickroll")
                .addBinaryBody("upload_file", binaryData,
                        ContentType.IMAGE_JPEG, "filename")
                .addTextBody("tos", "agree")
                .build();

        var httpPost = new HttpPost("http://localhost:" + 8456);
        httpPost.setEntity(entity);
        var httpresponse = httpclient.execute(httpPost);
        var sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        var content = "";
        while (sc.hasNext()) {
            content += sc.nextLine();
        }
        assertTrue(content.contains("\"X-block-recursive\":[\"http://localhost:8456/\"]"));
        assertTrue(content.contains("5555555555"));
        assertTrue(content.contains(new String(Base64.encode(binaryData))));
        assertTrue(content.contains("agree"));
        assertTrue(content.contains("tos"));
        assertTrue(content.contains("clip"));
    }
}
