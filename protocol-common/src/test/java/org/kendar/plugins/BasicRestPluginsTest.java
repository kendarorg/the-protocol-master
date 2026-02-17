package org.kendar.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.exceptions.PluginException;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.dtos.RestPluginsCallResult;
import org.kendar.plugins.settings.BasicRestPluginsPluginSettings;
import org.kendar.plugins.settings.dtos.RestPluginsInterceptor;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

public class BasicRestPluginsTest {
    private final JsonMapper mapper = new JsonMapper();
    private BasicRestPluginsPlugin target;
    private BasicRestPluginsPluginSettings targetSettings;
    private HttpServer server;

    @BeforeEach
    public void setUp() throws Exception {
        target = new BasicRestPluginsPlugin(mapper) {
            @Override
            public String getProtocol() {
                return "test";
            }
        };
        targetSettings = new BasicRestPluginsPluginSettings();
        target.setSettings(targetSettings);
        target.setActive(true);
    }

    @AfterEach
    void tearDown() {
        try {
            server.stop(0);
        } catch (Exception e) {
            //NOOP
        }
    }

    protected void startServer(String path, int port, boolean blocking, Throwable exception) {
        try {
            var threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext(path, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    handleMessage(exchange, blocking, exception);
                }

            });
            server.setExecutor(threadPoolExecutor);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void handleMessage(HttpExchange exchange, boolean blocking, Throwable exception) throws IOException {
        StringBuilder sb = new StringBuilder();
        var ios = exchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }
        var result = new RestPluginsCallResult();
        var response = new Response();
        try {
            response = mapper.deserialize(sb.toString(), Response.class);
            response.setResponseText(new TextNode("Response passed"));
        } catch (Exception e) {
            response.setResponseText(new TextNode("Response not passed"));
        }
        var returnCode = 200;
        if (exception != null) {
            response.setResponseText(new TextNode(exception.getMessage()));
            returnCode = 500;
            result.setWithError(true);
            result.setError(exception.toString());
        }
        result.setMessage(mapper.serialize(response));
        result.setBlocking(blocking);
        var responseBytes = mapper.serialize(result).getBytes();
        exchange.sendResponseHeaders(returnCode, responseBytes.length);
        var os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    @Test
    void inAndOut() {
        startServer("/test", 7125, true, null);
        var interceptor = new RestPluginsInterceptor();
        interceptor.setDestinationAddress("http://localhost:7125/test");
        interceptor.setInputType("Request");
        interceptor.setOutputType("Response");
        interceptor.setPhase(ProtocolPhase.PRE_CALL);
        interceptor.setBlockOnException(true);
        targetSettings.getInterceptors().add(interceptor);
        target.handleSettingsChanged();

        var pluginContext = new PluginContext("test", "testType", 0, null);
        var in = new Request();
        var out = new Response();

        assertTrue(target.handle(pluginContext, ProtocolPhase.PRE_CALL, in, out));
        assertEquals("Response passed", out.getResponseText().textValue());
    }

    @Test
    void inAndOutNonBlocking() {
        startServer("/test", 7125, false, null);
        var interceptor = new RestPluginsInterceptor();
        interceptor.setDestinationAddress("http://localhost:7125/test");
        interceptor.setInputType("Request");
        interceptor.setOutputType("Response");
        interceptor.setPhase(ProtocolPhase.PRE_CALL);
        interceptor.setBlockOnException(true);
        targetSettings.getInterceptors().add(interceptor);
        target.handleSettingsChanged();

        var pluginContext = new PluginContext("test", "testType", 0, null);
        var in = new Request();
        var out = new Response();

        assertFalse(target.handle(pluginContext, ProtocolPhase.PRE_CALL, in, out));
        assertEquals("Response passed", out.getResponseText().textValue());
    }

    @Test
    void onlyOut() {
        startServer("/test", 7125, true, null);
        var interceptor = new RestPluginsInterceptor();
        interceptor.setDestinationAddress("http://localhost:7125/test");
        interceptor.setOutputType("Response");
        interceptor.setPhase(ProtocolPhase.PRE_CALL);
        interceptor.setBlockOnException(true);
        targetSettings.getInterceptors().add(interceptor);
        target.handleSettingsChanged();

        var pluginContext = new PluginContext("test", "testType", 0, null);

        var out = new Response();

        assertTrue(target.handle(pluginContext, ProtocolPhase.PRE_CALL, null, out));
        assertEquals("Response passed", out.getResponseText().textValue());
    }

    @Test
    void inOnly() {
        startServer("/test", 7125, true, null);
        var interceptor = new RestPluginsInterceptor();
        interceptor.setDestinationAddress("http://localhost:7125/test");
        interceptor.setInputType("Request");
        interceptor.setPhase(ProtocolPhase.PRE_CALL);
        interceptor.setBlockOnException(true);
        targetSettings.getInterceptors().add(interceptor);
        target.handleSettingsChanged();

        var pluginContext = new PluginContext("test", "testType", 0, null);
        var in = new Request();

        assertTrue(target.handle(pluginContext, ProtocolPhase.PRE_CALL, in, null));
    }

    @Test
    void exception() {
        startServer("/test", 7125, true, new Exception());
        var interceptor = new RestPluginsInterceptor();
        interceptor.setDestinationAddress("http://localhost:7125/test");
        interceptor.setInputType("Request");
        interceptor.setPhase(ProtocolPhase.PRE_CALL);
        interceptor.setBlockOnException(true);
        targetSettings.getInterceptors().add(interceptor);
        target.handleSettingsChanged();

        var pluginContext = new PluginContext("test", "testType", 0, null);
        var in = new Request();

        assertThrows(PluginException.class, () -> target.handle(pluginContext, ProtocolPhase.PRE_CALL, in, null));
    }

    @Test
    void exceptionNonBlocking() {
        startServer("/test", 7125, true, new Exception());
        var interceptor = new RestPluginsInterceptor();
        interceptor.setDestinationAddress("http://localhost:7125/test");
        interceptor.setInputType("Request");
        interceptor.setPhase(ProtocolPhase.PRE_CALL);
        interceptor.setBlockOnException(false);
        targetSettings.getInterceptors().add(interceptor);
        target.handleSettingsChanged();

        var pluginContext = new PluginContext("test", "testType", 0, null);
        var in = new Request();

        assertFalse(target.handle(pluginContext, ProtocolPhase.PRE_CALL, in, null));
    }
}
