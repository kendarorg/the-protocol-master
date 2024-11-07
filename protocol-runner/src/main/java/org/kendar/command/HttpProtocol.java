package org.kendar.command;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.kendar.HttpTcpServer;
import org.kendar.filters.PluginDescriptor;
import org.kendar.http.MasterHandler;
import org.kendar.http.plugins.*;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.http.settings.HttpSSLSettings;
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
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HttpProtocol extends CommonProtocol {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocol.class);

    private static HttpsServer createHttpsServer(CertificatesManager certificatesManager,
                                                 InetSocketAddress sslAddress, int backlog, String cname, String der,
                                                 String key, List<String> hosts) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, hosts, cname, der, key);
        return httpsServer;
    }


    private static SimpleRewriterConfig loadRewritersConfiguration(HttpProtocolSettings settings) {
        var proxyConfig = new SimpleRewriterConfig();
        for(var i=0;i<settings.getRewrites().size();i++){
            var rw = settings.getRewrites().get(i);
            if(rw.getWhen()==null||rw.getThen()==null){
                continue;
            }
            var remoteServerStatus = new RemoteServerStatus(i + "",
                    rw.getWhen(),
                    rw.getThen(),
                    rw.getTest());
            if (rw.getTest() == null || rw.getTest().isEmpty()) {
                remoteServerStatus.setRunning(true);
                remoteServerStatus.setForce(true);
            } else {
                remoteServerStatus.setRunning(true);
                remoteServerStatus.setForce(rw.isForceActive());
            }
            proxyConfig.getProxies().add(remoteServerStatus);
        }
        return proxyConfig;
    }

    @Override
    public void run(String[] args, boolean isExecute, GlobalSettings go,
                    Options options, HashMap<String, List<PluginDescriptor>> filters) throws Exception {
        options.addOption(createOpt("ht", "http", true, "Http port (def 4080)"));
        options.addOption(createOpt("hs", "https", true, "Https port (def 4443)"));
        options.addOption(createOpt("prx", "proxy", true, "Http/s proxy port (def 9999)"));
        options.addOption(createOpt("ap", "apis", true, "The base url for special TPM controllers (def specialApisRoot)"));
        options.addOption(createOpt("prp", "replay", false, "Replay from log/replay source."));
        options.addOption(createOpt("prc", "record", false, "Record to log/replay source."));
        options.addOption(createOpt("plid", "replayid", true, "Set an id for the replay instance (default to timestamp_uuid)."));
        options.addOption(createOpt("ae", "allowExternal", false, "Allow external calls during replay ."));

        options.addOption(createOpt("cn", "cname", true, "Root cname"));
        options.addOption("der", true, "Root certificate");
        options.addOption("key", true, "Root certificate keys");

        options.addOption(createOpt("be", "blockExternal", false, "Set if should block external sites replaying"));

        options.addOption("showError", true, "The error to show (404/500 etc) default 0/none");
        options.addOption("errorPercent", true, "The error percent to generate (default 0)");
        options.addOption("errorMessage", true, "The error message");
        if (!isExecute) return;
        setCommonData(args, options, go, new HttpProtocolSettings());
    }

    protected void setCommonData(String[] args, Options options, GlobalSettings ini,
                                 HttpProtocolSettings section) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        ini.getProtocols().put(getId(),section);
        section.setProtocol(getId());
        section.setHttp(Integer.parseInt(cmd.getOptionValue("http", "4080")));
        section.setHttps(Integer.parseInt(cmd.getOptionValue("https", "4443")));
        section.setProxy(Integer.parseInt(cmd.getOptionValue("proxy", "9999")));
        section.setApis(cmd.getOptionValue("apis", "specialApisRoot"));
        var sslSettings = new HttpSSLSettings();
        sslSettings.setCname(cmd.getOptionValue("cname", "C=US,O=Local Development,CN=local.org"));
        sslSettings.setDer(cmd.getOptionValue("der", "resource://certificates/ca.der"));
        sslSettings.setKey(cmd.getOptionValue("key", "resource://certificates/ca.key"));
        section.setSSL(sslSettings);

        if (cmd.hasOption("replay")) {
            var pl = new HttpMockPluginSettings();
            pl.setPlugin("mock-plugin");
            pl.setReplay(true);
            pl.setRespectCallDuration(cmd.hasOption("cdt"));
            pl.setReplayId(cmd.getOptionValue("replayid", UUID.randomUUID().toString()));
            pl.setBlockExternal(!cmd.hasOption("allowExternal"));
            section.getPlugins().put("mock-plugin", pl);
        } else if (cmd.hasOption("record")) {
            var pl = new HttpRecordPluginSettings();
            pl.setPlugin("recording-plugin");
            pl.setRecord(true);
            section.getPlugins().put("recording-plugin", pl);
        }
        if (cmd.hasOption("showError") && cmd.hasOption("errorPercent")) {
            var pl = new HttpErrorPluginSettings();
            pl.setShowError(Integer.parseInt(cmd.getOptionValue("showError", "0")));
            pl.setErrorPercent(Integer.parseInt(cmd.getOptionValue("errorPercent", "0")));
            pl.setErrorMessage(cmd.getOptionValue("errorMessage", "Error"));
        }
    }

    @Override
    public String getDefaultPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void start(ConcurrentHashMap<String, TcpServer> protocolServer,
                      String sectionKey, GlobalSettings ini, ProtocolSettings pset, StorageRepository storage,
                      List<PluginDescriptor> filters, Supplier<Boolean> stopWhenFalseAction) throws Exception {
        var settings = (HttpProtocolSettings) pset;
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
            log.debug("Started waiter");

            var port = OptionsManager.getOrDefault(settings.getHttp(), 4080);
            var httpsPort = OptionsManager.getOrDefault(settings.getHttps(),  4443);
            var proxyPort = OptionsManager.getOrDefault(settings.getProxy(),  9999);
//        log.info("LISTEN HTTP: " + port);
//        log.info("LISTEN HTTPS: " + httpsPort);
//        log.info("LISTEN PROXY: " + proxyPort);
            var backlog = 60;
            var useCachedExecutor = true;
            var address = new InetSocketAddress(port);
            var sslAddress = new InetSocketAddress(httpsPort);


            // initialise the HTTP server
            var proxyConfig = loadRewritersConfiguration(settings);
            var dnsHandler = new DnsMultiResolverImpl();
            var connectionBuilder = new ConnectionBuilderImpl(dnsHandler);
            var requestResponseBuilder = new RequestResponseBuilderImpl();


            var httpServer = HttpServer.create(address, backlog);
            log.debug("Http created");
            ps.setStop(() -> {
                waiterBlock.set(false);
                httpServer.stop(0);
            });

            var sslDer = OptionsManager.getOrDefault(settings.getSSL().getDer(), "resource://certificates/ca.der");
            var sslKey = OptionsManager.getOrDefault(settings.getSSL().getKey(), "resource://certificates/ca.key");
            var cname = OptionsManager.getOrDefault(settings.getSSL().getCname(), "C=US,O=Local Development,CN=local.org");

            var certificatesManager = new CertificatesManager(new FileResourcesUtils());
            var httpsServer = createHttpsServer(certificatesManager,
                    sslAddress, backlog, cname, sslDer, sslKey,settings.getSSL().getHosts());
            log.debug("Https created");
            ps.setStop(() -> {
                waiterBlock.set(false);
                httpsServer.stop(0);
                httpServer.stop(0);
            });

            var proxy = new ProxyServer(proxyPort)
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
            ps.setStop(() -> {
                waiterBlock.set(false);
                proxy.terminate();
                httpsServer.stop(0);
                httpServer.stop(0);
            });
            log.debug("Proxy created");
            new Thread(proxy).start();


            for (var i = filters.size() - 1; i >= 0; i--) {
                var filter = filters.get(i);
                filter.initialize(ini, pset);
            }

            var globalFilter = new GlobalPlugin();
            globalFilter.initialize(ini,pset);
            filters.add(globalFilter);

            globalFilter.setFilters(filters);
            globalFilter.setServer(httpServer, httpsServer);
            globalFilter.setShutdownVariable(stopWhenFalse);
            log.debug("Filters added");
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
            log.debug("Servers started");


            protocolServer.put(sectionKey, ps);
        } catch (Exception ex) {
            try {
                log.error(ex.getMessage(), ex);
                var sr = protocolServer.get(sectionKey);
                sr.stop();
            } catch (Exception xx) {

            }
        }
    }

    @Override
    public String getId() {
        return "http";
    }

    @Override
    public Class<?> getSettingsClass() {
        return HttpProtocolSettings.class;
    }
}
