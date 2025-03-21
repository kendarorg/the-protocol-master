package org.kendar.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.kendar.apis.converters.RequestResponseBuilderImpl;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.http.events.SSLAddHostEvent;
import org.kendar.http.events.SSLRemoveHostEvent;
import org.kendar.http.ssl.CertificatesManager;
import org.kendar.http.utils.ConnectionBuilderImpl;
import org.kendar.http.utils.callexternal.ExternalRequesterImpl;
import org.kendar.http.utils.dns.DnsMultiResolverImpl;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyServer;
import org.kendar.server.KendarHttpsServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.FileResourcesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@TpmService(tags = "http")
public class HttpProtocol extends NetworkProtoDescriptor {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocol.class);
    private final GlobalSettings globalSettings;
    private final List<BasePluginDescriptor> plugins;
    private final HttpProtocolSettings settings;
    private ProxyServer proxy;
    private HttpsServer httpsServer;
    private HttpServer httpServer;
    private boolean httpRunning;
    private boolean httpsRunning;

    @TpmConstructor
    public HttpProtocol(GlobalSettings ini, HttpProtocolSettings settings,
                        @TpmNamed(tags = "http") List<BasePluginDescriptor> plugins) {
        this.globalSettings = ini;
        this.settings = settings;
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            var specificPluginSetting = settings.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (specificPluginSetting != null || AlwaysActivePlugin.class.isAssignableFrom(plugin.getClass())) {
                ((ProtocolPluginDescriptor) plugin).initialize(ini, settings, specificPluginSetting);
                plugin.refreshStatus();
            } else {
                plugins.remove(i);
            }
        }
        this.plugins = plugins;
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

    @Override
    public ProtocolSettings getSettings() {
        return settings;
    }

    public Map<String, Integer> getPorts() {
        return Map.of("proxy", settings.getProxy(), "http", settings.getHttp(), "https", settings.getHttps());
    }

    @Override
    public List<BasePluginDescriptor> getPlugins() {
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
            var protocol = this;
            int port = getOrDefault(settings.getHttp(), 4080);
            int httpsPort = getOrDefault(settings.getHttps(), 4443);
            var proxyPort = getOrDefault(settings.getProxy(), 9999);

            var backlog = 60;
            var useCachedExecutor = true;
            var address = new InetSocketAddress(port);
            var sslAddress = new InetSocketAddress(httpsPort);


            // initialise the HTTP server
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


            EventsQueue.register("http-" + getSettings().getProtocolInstanceId(), (e) -> {
                try {

                    certificatesManager.setupSll(httpsServer, List.of(e.getHost()), cname, sslDer, sslKey);
                } catch (Exception ex) {
                    throw new TPMProtocolException("Error updating ssl " + protocol.getSettings().getProtocolInstanceId(), ex);
                }
            }, SSLAddHostEvent.class);
            EventsQueue.register("http-" + getSettings().getProtocolInstanceId(), (e) -> {
                try {
                    certificatesManager.unsetSll(httpsServer, List.of(e.getHost()), cname, sslDer, sslKey);
                } catch (Exception ex) {
                    throw new TPMProtocolException("Error updating ssl " + protocol.getSettings().getProtocolInstanceId(), ex);
                }
            }, SSLRemoveHostEvent.class);

            var concurrentHashMap = new ConcurrentHashMap<String, String>();
            proxy = new ProxyServer(proxyPort)
                    .withHttpRedirect(port).withHttpsRedirect(httpsPort)
                    .withDnsResolver(host -> {
                        try {
                            if (!concurrentHashMap.containsKey(host)) {
                                concurrentHashMap.put(host, host);
                                EventsQueue.send(new ReportDataEvent(
                                        getSettings().getProtocolInstanceId(),
                                        "dns",
                                        host,
                                        -1,
                                        new Date().getTime(),
                                        0,
                                        Map.of()
                                ));
                            }
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

            var handler = new MasterHandler(
                    new PluginClassesHandlerImpl(plugins, this),
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
            throw new TPMProtocolException(ex);
        }
    }
}
