package org.kendar.command;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.cli.Options;
import org.kendar.HttpTcpServer;
import org.kendar.filters.FilterDescriptor;
import org.kendar.http.MasterHandler;
import org.kendar.http.plugins.ErrorFilter;
import org.kendar.http.plugins.GlobalFilter;
import org.kendar.http.plugins.MockFilter;
import org.kendar.http.plugins.RecordingFilter;
import org.kendar.http.utils.ConnectionBuilderImpl;
import org.kendar.http.utils.callexternal.ExternalRequesterImpl;
import org.kendar.http.utils.converters.RequestResponseBuilderImpl;
import org.kendar.http.utils.dns.DnsMultiResolverImpl;
import org.kendar.http.utils.filters.FilteringClassesHandlerImpl;
import org.kendar.http.utils.rewriter.RemoteServerStatus;
import org.kendar.http.utils.rewriter.SimpleRewriterConfig;
import org.kendar.http.utils.rewriter.SimpleRewriterHandlerImpl;
import org.kendar.http.utils.ssl.CertificatesManager;
import org.kendar.http.utils.ssl.FileResourcesUtils;
import org.kendar.proxy.ProxyServer;
import org.kendar.server.KendarHttpsServer;
import org.kendar.server.TcpServer;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.kendar.utils.ini.Ini;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HttpProtocol extends CommonProtocol{
    @Override
    public void run(String[] args, boolean isExecute, Ini go, Options options) throws Exception {
        options.addOption("http", true, "Http port (def 4080)");
        options.addOption("https", true, "Https port (def 4443)");
        options.addOption("proxy", true, "Http/s proxy port (def 9999)");
        options.addOption("apis", true, "The base url for special TPM controllers (def specialApisRoot)");

        options.addOption("cname", true, "Root cname");
        options.addOption("der", true, "Root certificate");
        options.addOption("key", true, "Root certificate keys");


        options.addOption("record", false, "Set if recording");

        options.addOption("replay", false, "Set if replaying");
        options.addOption("blockExternal", false, "Set if should block external sites replaying");

        options.addOption("showError", true, "The error to show (404/500 etc)");
        options.addOption("errorPercent", true, "The error percent to generate (default 50)");
        if(!isExecute)return;
        setData(args,options,go);
    }


    private void setData(String[] args, Options options, Ini ini) throws Exception {

    }

    @Override
    public String getDefaultPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String sectionKey, Ini ini, String protocol,
                      StorageRepository storage, ArrayList<FilterDescriptor> filters, Supplier<Boolean> stopWhenFalseAction) {
        var ps = new HttpTcpServer(null);
        try {

            AtomicBoolean stopWhenFalse = new AtomicBoolean(true);
            AtomicBoolean waiterBlock = new AtomicBoolean(true);

            ps.setRunner(() -> {
                new Thread(() -> {
                    while (stopWhenFalseAction.get() && waiterBlock.get()) {
                        Sleeper.sleep(100);
                    }
                    stopWhenFalse.set(false);
                }).start();
            });

            ps.setIsRunning(() -> stopWhenFalse.get());

            var port = ini.getValue(sectionKey, "http.port", Integer.class, 8085);
            var httpsPort = ini.getValue(sectionKey, "https.port", Integer.class, port + 400);
            var proxyPort = ini.getValue(sectionKey, "port.proxy", Integer.class, 9999);
//        log.info("LISTEN HTTP: " + port);
//        log.info("LISTEN HTTPS: " + httpsPort);
//        log.info("LISTEN PROXY: " + proxyPort);
            var backlog = 60;
            var useCachedExecutor = true;
            var address = new InetSocketAddress(port);
            var sslAddress = new InetSocketAddress(httpsPort);


            // initialise the HTTP server
            var proxyConfig = loadRewritersConfiguration(sectionKey, ini);
            var dnsHandler = new DnsMultiResolverImpl();
            var connectionBuilder = new ConnectionBuilderImpl(dnsHandler);
            var requestResponseBuilder = new RequestResponseBuilderImpl();

            var certificatesManager = new CertificatesManager(new FileResourcesUtils());
            var httpServer = HttpServer.create(address, backlog);
            ps.setStop(() -> {
                waiterBlock.set(false);
                httpServer.stop(0);
            });

            var der = ini.getValue(sectionKey + "-ssl", "der", String.class, "resources://certificates/ca.der");
            var key = ini.getValue(sectionKey + "-ssl", "key", String.class, "resources://certificates/ca.key");
            var cname = ini.getValue(sectionKey + "-ssl", "cname", String.class, "C=US,O=Local Development,CN=local.org");

            var httpsServer = createHttpsServer(certificatesManager, sslAddress, backlog, cname, der, key);
            ps.setStop(() -> {
                waiterBlock.set(false);
                httpsServer.stop(0);
                httpServer.stop(0);
            });

            var proxy = new ProxyServer(proxyPort)
                    .withHttpRedirect(port).withHttpsRedirect(httpsPort)
                    .withDnsResolver(host -> {
                        try {
                            certificatesManager.setupSll(httpsServer, List.of(host), cname, der, key);
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
            ps.setStop(() -> {
                waiterBlock.set(false);
                proxy.stop();
                httpsServer.stop(0);
                httpServer.stop(0);
            });
            new Thread(proxy).start();

            var globalFilter = new GlobalFilter();

            filters.add(globalFilter);
            filters.add(new RecordingFilter());
            filters.add(new ErrorFilter());
            filters.add(new MockFilter());
            for (var i = filters.size() - 1; i >= 0; i--) {
                var filter = filters.get(i);
                var section = ini.getSection(sectionKey + "-" + filter.getId());
                if (!filter.getId().equalsIgnoreCase("global") &&
                        !ini.getValue(sectionKey + "-" + filter.getId(), "active", Boolean.class, false)) {
                    filters.remove(i);
                    continue;
                }
                //log.info("EXTENSION: " + filter.getId());
                filter.initialize(section);
            }
            globalFilter.setFilters(filters);
            globalFilter.setServer(httpServer, httpsServer);
            globalFilter.setShutdownVariable(stopWhenFalse);
            var handler = new MasterHandler(
                    new FilteringClassesHandlerImpl(filters),
                    new SimpleRewriterHandlerImpl(proxyConfig, dnsHandler),
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


            protocolServer.put(sectionKey, ps);
        }catch (Exception ex){
            try {
                var sr = protocolServer.get(sectionKey);
                sr.stop();
            }catch (Exception xx){

            }
        }
    }

    private static HttpsServer createHttpsServer(CertificatesManager certificatesManager, InetSocketAddress sslAddress, int backlog, String cname, String der, String key) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, List.of(),cname, der, key);
        return httpsServer;
    }

    private static SimpleRewriterConfig loadRewritersConfiguration(String key, Ini ini) {
        var proxyConfig = new SimpleRewriterConfig();
        for (var id = 0; id < 255; id++) {
            var when = ini.getValue(key+"-rewriter", "rewrite." + id + ".when", String.class);
            var where = ini.getValue(key+"-rewriter", "rewrite." + id + ".where", String.class);
            var test = ini.getValue(key+"-rewriter", "rewrite." + id + ".test", String.class);
            if (when == null || where == null) {
                continue;
            }
            var remoteServerStatus = new RemoteServerStatus(id + "",
                    when,
                    where,
                    test);
            if(test==null||test.isEmpty()) {
                remoteServerStatus.setRunning(true);
                remoteServerStatus.setForce(true);
            }else{

                remoteServerStatus.setRunning(false);
                remoteServerStatus.setForce(false);
            }
            proxyConfig.getProxies().add(remoteServerStatus);
        }
        return proxyConfig;
    }

    @Override
    public String getId() {
        return "http";
    }
}
