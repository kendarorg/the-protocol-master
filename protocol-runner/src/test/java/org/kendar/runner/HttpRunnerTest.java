package org.kendar.runner;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.runner.utils.SimpleHttpServer;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpRunnerTest extends BasicTest {
    static int FAKE_PORT_HTTP = 8087;
    static int FAKE_PORT_HTTPS = 8487;
    static int FAKE_PORT_PROXY = 9999;
    //private final AtomicBoolean runTheServer = new AtomicBoolean(true);

    private static boolean listening(int port) throws IllegalStateException {
        try (Socket ignored = new Socket("localhost", port)) {
            return true;
        } catch (ConnectException e) {
            return false;
        } catch (IOException e) {
            throw new IllegalStateException("Error while trying to check open port", e);
        }
    }

    @BeforeEach
    public void beforeEach() throws IOException {

        Sleeper.sleep(500);
    }

    @AfterEach
    public void afterEach() {

        Main.stop();
        Sleeper.sleep(5000, () -> {
            var res = listening(8087) || listening(8487) || listening(9999);
            return !res;
        });
        System.out.println("COMPLETED");
    }

    @Test
    void asimpleTest() throws IOException {
        var simpleServer = new SimpleHttpServer();
        simpleServer.start(8456);
        var timestampForThisRun = "" + new Date().getTime();

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "asimpleTest").toString(),
                "-pluginsDir", Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-protocol", "http",
                "-http", "" + FAKE_PORT_HTTP,
                "-https", "" + FAKE_PORT_HTTPS,
                "-proxy", "" + FAKE_PORT_PROXY,
                "-record"
        };
        startAndHandleUnexpectedErrors(args);
        Sleeper.sleep(1000, () -> {
            var res = listening(8087) && listening(8487) && listening(9999);
            return res;
        });

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
    void googleTest() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        var args = new String[]{

                "-datadir", Path.of("target", "tests", "googleTest").toString(),
                "-pluginsDir", Path.of("target", "plugins").toString(),
                "-loglevel", "DEBUG",
                "-protocol", "http",
                "-http", "" + FAKE_PORT_HTTP,
                "-https", "" + FAKE_PORT_HTTPS,
                "-proxy", "" + FAKE_PORT_PROXY,
                "-record"
        };
        startAndHandleUnexpectedErrors(args);
        Sleeper.sleep(1000, () -> {
            var res = listening(8087) && listening(8487) && listening(9999);
            return res;
        });

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
    void googleTestIni() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        var args = new String[]{

                "-cfg", Path.of("src", "test", "resources", "google.json").toString()
        };
        startAndHandleUnexpectedErrors(args);
        Sleeper.sleep(1000, () -> {
            var res = listening(12080) && listening(12443) && listening(12081);
            return res;
        });

        final var sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        var proxy = new HttpHost("localhost", 12081, "http");
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
}
