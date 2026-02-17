package org.kendar.runner;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.*;
import org.kendar.Main;
import org.kendar.apis.base.Response;
import org.kendar.plugins.dtos.RestPluginCall;
import org.kendar.plugins.dtos.RestPluginsCallResult;
import org.kendar.tests.utils.TestSleeper;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import static java.util.regex.Matcher.quoteReplacement;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiSeleniumTest extends SeleniumTestBase {
    private static final String REGEXP = "/images/branding/[_a-zA-Z0-9]+/[_a-zA-Z0-9]+/[_a-zA-Z0-9]+\\.png";
    private static BasicTest bs;
    private final Pattern pattern = Pattern.compile(".*" + REGEXP + ".*");
    private HttpServer server;

    @AfterAll
    public static void cleanup() {
        Main.stop();
        tearDownAfterClassBase();
    }

    @BeforeAll
    public static void setup() throws Exception {
        setupDirectories("seleniumtest");
        try {
            Main.stop();
        } catch (Exception e) {
        }
        var args = new String[]{

                "-cfg", Path.of("src", "test", "resources", "selenium_settings.json").toString()
        };
        var sourceData = Path.of("src", "test", "resources", "data");
        var destData = Path.of("target", "data", "selenium");
        FileUtils.copyDirectory(sourceData.toFile(), destData.toFile());
        sourceData = Path.of("..", "target", "plugins");
        destData = Path.of("target", "data", "plugins");
        FileUtils.copyDirectory(sourceData.toFile(), destData.toFile());
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
    }

    @AfterEach
    public void tearDownAfterEach() throws Exception {
        writeScenario();
        tearDownAfterEachBase();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) throws Exception {
        beforeEachBase(testInfo, 9999);
    }

    //RUN_VISIBLE=true
    @Test
    public void basicTest() throws Exception {
        navigateTo("http://localhost:8095/", false);
        TestSleeper.sleep(1000);
        navigateTo("http://localhost:8095/storage", false);
        TestSleeper.sleep(1000);
        navigateTo("http://localhost:8095/recording", false);
        TestSleeper.sleep(1000);
        navigateTo("http://localhost:8095/protocols", false);
        TestSleeper.sleep(1000);
        navigateTo("http://localhost:8095/plugins", false);
        TestSleeper.sleep(1000);
        navigateTo("http://localhost:8095/globalpl", false);
        TestSleeper.sleep(1000);
        navigateTo("http://localhost:8095/swagger-ui/index.html#/", false);
        TestSleeper.sleep(1000);
    }

    protected void startServer(String path, int port) {
        try {
            var threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext(path, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    handleMessage(exchange);
                }

            });
            server.setExecutor(threadPoolExecutor);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        var ios = exchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }
        var result = new RestPluginsCallResult();

        var call = mapper.deserialize(sb.toString(), RestPluginCall.class);
        var response = mapper.deserialize(call.getOutput(), Response.class);
        try {
            var content = response.getResponseText().textValue();
            if (!content.isEmpty() && pattern.matcher(content).find()) {
                content = content.replaceAll(REGEXP, quoteReplacement(
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9c" +
                                "/Bing_Fluent_Logo.svg/120px-Bing_Fluent_Logo.svg.png"));
                response.setResponseText(new TextNode(content));
            }
            result.setBlocking(true);
            result.setMessage(mapper.serialize(response));
        } catch (Exception ex) {
            result.setBlocking(false);
            result.setWithError(true);
            result.setMessage("Not valid");
        }

        var responseBytes = mapper.serialize(result).getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);
        var os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    @Test
    public void httpRestPluginsPlugin() throws Exception {
        startServer("/test", 38878);
        var httpclient = HttpClients.createDefault();
        try {
            getRequest("http://localhost:8095/api/protocols/http-01/plugins/rest-plugins-plugin/start", httpclient, String.class);

            navigateTo("https://www.google.com", false);
            Sleeper.sleep(1000);
            alertWhenHumanDriven("Showing the bing logo :)");
            assertTrue(getDriver().getPageSource().contains("Bing_Fluent_Logo"));
        } finally {
            getRequest("http://localhost:8095/api/protocols/http-01/plugins/rest-plugins-plugin/stop", httpclient, String.class);
        }

    }
}
