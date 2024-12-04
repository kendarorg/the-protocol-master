package org.kendar.http;

import org.junit.jupiter.api.Test;
import org.kendar.http.plugins.HttpRewritePlugin;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.apis.base.Request;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.NullStorageRepository;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewriterPluginTest {
    @Test
    void doTest() {
        var settings = new RewritePluginSettings();
        settings.setRewritesFile(Path.of("src", "test", "resources", "rewrite.json").toAbsolutePath().toString());
        settings.setActive(true);
        var target = new HttpRewritePlugin();
        var global = new GlobalSettings();
        global.putService("storage", new NullStorageRepository());

        target.initialize(global, new HttpProtocolSettings(), settings);
        var phase = ProtocolPhase.CONNECT;
        var pc = new PluginContext("http", "type", 0, null);
        var request = new Request();
        request.setProtocol("http");


        request.setProtocol("http");
        request.setHost("localhost");
        request.setPort(8080);
        request.setPath("/");
        target.handle(pc, phase, request, null);
        assertEquals("/", request.getPath());
        assertEquals("localhost", request.getHost());
        assertEquals("http", request.getProtocol());


        request.setProtocol("http");
        request.setHost("localhost");
        request.setPort(8080);
        request.setPath("/external/google/test");
        target.handle(pc, phase, request, null);
        assertEquals("/test", request.getPath());
        assertEquals("www.google.com", request.getHost());
        assertEquals("https", request.getProtocol());

        request.setProtocol("http");
        request.setHost("localhost");
        request.setPort(8080);
        request.setPath("/regex/microsoft/trial");
        target.handle(pc, phase, request, null);
        assertEquals("/trial", request.getPath());
        assertEquals("www.microsoft.com", request.getHost());
        assertEquals("https", request.getProtocol());

        request.setProtocol("http");
        request.setHost("localhost");
        request.setPort(8080);
        request.setPath("/multireg/linkedin/test/123456/trial");
        target.handle(pc, phase, request, null);
        assertEquals("/test/123456/trial", request.getPath());
        assertEquals("www.linkedin.com", request.getHost());
        assertEquals("https", request.getProtocol());
    }
}
