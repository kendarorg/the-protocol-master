package org.kendar.http;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kendar.http.utils.ConsumeException;
import org.kendar.utils.FileResourcesUtils;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleTest extends BasicTest {






    private static CloseableHttpClient createHttpClient(HttpClientBuilder custom) {
        var proxy = new HttpHost("localhost", FAKE_PORT_PROXY, "http");
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        var httpclient = custom.setRoutePlanner(routePlanner).build();
        return httpclient;
    }

    private static byte[] buildBytesData(){
        try {
            var frf = new FileResourcesUtils();
            return frf.getFileFromResourceAsByteArray("resource://image.gif");
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static class NullEntity extends AbstractHttpEntity implements Cloneable{
        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return null;
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {

        }

        @Override
        public boolean isStreaming() {
            return false;
        }
    }

    private static ByteArrayEntity buildByteEntity(){
        try {


            var  entity = new ByteArrayEntity(buildBytesData());
            return entity;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }

    private static String getContentString(HttpResponse httpresponse){
        try {
            var sc = new Scanner(httpresponse.getEntity().getContent());

            //Printing the status line
            assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
            var content = "";
            while (sc.hasNext()) {
                content += sc.nextLine();
            }
            return content;
        }catch (Exception ex){
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

    private static CloseableHttpClient createHttpsHttpClient() throws Exception {
        final var sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        var httpclient = createHttpClient(HttpClients.custom()
                .setSSLContext(sslContext)
                .setConnectionManager(
                        new PoolingHttpClientConnectionManager(
                                RegistryBuilder.<ConnectionSocketFactory>create()
                                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                                        .register("https", new SSLConnectionSocketFactory(sslContext,
                                                NoopHostnameVerifier.INSTANCE))
                                        .build()
                        )));
        return httpclient;
    }

    private static byte[] getConfusingByteArray(){
        return new byte[]{1, 2, 3, 4, 5, '\r', '\n'};
    }

    @ParameterizedTest
    @MethodSource("provideTestSituations")
    void allSamplesTest(String description, HttpEntity entity, HttpRequestBase httpPost , Map<String,String> headers, ConsumeException<HttpResponse> consumer) throws Exception {
        var recordPlugin = baseProtocol.getPlugins().stream().filter(a -> a.getId().equalsIgnoreCase("record-plugin")).findFirst();
        recordPlugin.get().setActive(true);

        var httpclient = createHttpClient(HttpClients.custom());

        if(HttpEntityEnclosingRequestBase.class.isAssignableFrom(httpPost.getClass()) ) {
            ((HttpEntityEnclosingRequestBase)httpPost).setEntity(entity);
        }
        for(var h: headers.entrySet()){
            httpPost.setHeader(h.getKey(), h.getValue());
        }
        var httpresponse = httpclient.execute(httpPost);
        assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
        consumer.accept(httpresponse);
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
                            withQuery(new HttpGet("http://localhost:" + 8456 + "/jsonized"),Map.of("par1","par1Value")),
                            Map.of(),
                            (ConsumeException<HttpResponse>) httpResponse -> {
                                var content = getContentString(httpResponse);
                                assertTrue(content.contains("par1Value"));
                            })
            );
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static HttpRequestBase withQuery(HttpRequestBase req, Map<String, String> queryParms) {
        try {
            var uriBuilder = new URIBuilder(req.getURI());
            for (var q : queryParms.entrySet()) {
                uriBuilder.addParameter(q.getKey(), q.getValue());
            }
            req.setURI(uriBuilder.build());
            return req;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
