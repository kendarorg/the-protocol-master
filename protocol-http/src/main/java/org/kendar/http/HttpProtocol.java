package org.kendar.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.kendar.apis.converters.RequestResponseBuilderImpl;
import org.kendar.http.plugins.SSLDummyPlugin;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.http.ssl.CertificatesManager;
import org.kendar.http.utils.ConnectionBuilderImpl;
import org.kendar.http.utils.callexternal.ExternalRequesterImpl;
import org.kendar.http.utils.dns.DnsMultiResolverImpl;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyServer;
import org.kendar.server.KendarHttpsServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.FileResourcesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class HttpProtocol extends NetworkProtoDescriptor {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocol.class);
    private final GlobalSettings globalSettings;
    private final List<ProtocolPluginDescriptor> plugins;
    private final HttpProtocolSettings settings;
    private ProxyServer proxy;
    private HttpsServer httpsServer;
    private HttpServer httpServer;
    private boolean httpRunning;
    private boolean httpsRunning;

    public HttpProtocol(GlobalSettings globalSettings, HttpProtocolSettings settings, List<ProtocolPluginDescriptor> plugins) {

        this.globalSettings = globalSettings;
        this.settings = settings;
        this.plugins = new ArrayList<>(plugins);
        var sslPlugin = new SSLDummyPlugin();
        sslPlugin.setActive(true);
        this.plugins.add(sslPlugin);
        //Disable logging for apache http client
        java.util.logging.Logger.getLogger("org.apache.http.client").setLevel(Level.OFF);

    }

    private static <T> T getOrDefault(Object value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    private static HttpsServer createHttpsServer(CertificatesManager certificatesManager,
                                                 InetSocketAddress sslAddress, int backlog, String cname, String der,
                                                 String key, List<String> hosts) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, hosts, cname, der, key);
        return httpsServer;
    }

    public List<ProtocolPluginDescriptor> getPlugins() {
        return plugins;
    }

    @Override
    public boolean isWrapper() {
        return true;
    }

    @Override
    public boolean isBe() {
        return false;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    protected void initializeProtocol() {

    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId) {
        return null;
    }

    @Override
    public void terminate() {
        var terminatedPlugins = new HashSet<>();
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            if (plugin.isActive() && !terminatedPlugins.contains(plugin)) {
                plugin.terminate();
                terminatedPlugins.add(plugin);
            }
        }
        proxy.terminate();
        httpsServer.stop(0);
        httpServer.stop(0);
        httpRunning = false;
        httpsRunning = false;
    }

    @Override
    public boolean isWrapperRunning() {
        return proxy.isRunning() && httpRunning && httpsRunning;
    }

    @Override
    public void start() {
        try {
            int port = getOrDefault(settings.getHttp(), 4080);
            int httpsPort = getOrDefault(settings.getHttps(), 4443);
            var proxyPort = getOrDefault(settings.getProxy(), 9999);

            var backlog = 60;
            var useCachedExecutor = true;
            var address = new InetSocketAddress(port);
            var sslAddress = new InetSocketAddress(httpsPort);


            // initialise the HTTP server
//            var proxyConfig = loadRewritersConfiguration(settings);
            var dnsHandler = new DnsMultiResolverImpl();
            var connectionBuilder = new ConnectionBuilderImpl(dnsHandler);
            var requestResponseBuilder = new RequestResponseBuilderImpl();


            httpServer = HttpServer.create(address, backlog);
            log.info("[CL>TP][IN] Listening on *.:{} Http", port);

            var sslDer = getOrDefault(settings.getSSL().getDer(), "resource://certificates/ca.der");
            var sslKey = getOrDefault(settings.getSSL().getKey(), "resource://certificates/ca.key");
            var cname = getOrDefault(settings.getSSL().getCname(), "C=US,O=Local Development,CN=local.org");

            var certificatesManager = new CertificatesManager(new FileResourcesUtils());
            httpsServer = createHttpsServer(certificatesManager,
                    sslAddress, backlog, cname, sslDer, sslKey, settings.getSSL().getHosts());
            log.info("[CL>TP][IN] Listening on *.:{} Https", httpsPort);


            proxy = new ProxyServer(proxyPort)
                    .withHttpRedirect(port).withHttpsRedirect(httpsPort)
                    .withDnsResolver(host -> {
                        try {
                            certificatesManager.setupSll(httpsServer, List.of(host), cname, sslDer, sslKey);
                        } catch (Exception e) {
                            return host;
                        }
                        return "127.0.0.1";
                    }).
                    ignoringHosts("static.chartbeat.com").
                    ignoringHosts("detectportal.firefox.com").
                    ignoringHosts("firefox.settings.services.mozilla.com").
                    ignoringHosts("incoming.telemetry.mozilla.org").
                    ignoringHosts("push.services.mozilla.com");

            proxy.start();
            log.info("[CL>TP][IN] Listening on *.:{} Http Proxy", proxyPort);


            for (var i = plugins.size() - 1; i >= 0; i--) {
                var plugin = plugins.get(i);
                var specificPluginSetting = settings.getPlugin(plugin.getId(), plugin.getSettingClass());
                if (specificPluginSetting != null) {
                    plugin.initialize(globalSettings, settings, specificPluginSetting);
                }
            }

            var handler = new MasterHandler(
                    new PluginClassesHandlerImpl(plugins),
                    new RequestResponseBuilderImpl(),
                    new ExternalRequesterImpl(requestResponseBuilder, dnsHandler, connectionBuilder),
                    connectionBuilder);

            httpServer.createContext("/", handler);
            httpsServer.createContext("/", handler);
            if (useCachedExecutor) {
                httpServer.setExecutor(Executors.newCachedThreadPool());
                httpsServer.setExecutor(Executors.newCachedThreadPool());
            } else {
                httpServer.setExecutor(null); // creates a default executor
                httpsServer.setExecutor(null);
            }
            httpsServer.start();
            httpServer.start();
            log.debug("[CL>TP][IN] Servers Started");
            httpRunning = true;
            httpsRunning = true;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
