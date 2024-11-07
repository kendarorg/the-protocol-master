package org.kendar.command;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.kendar.HttpTcpServer;
import org.kendar.filters.AlwaysActivePlugin;
import org.kendar.filters.PluginDescriptor;
import org.kendar.http.MasterHandler;
import org.kendar.http.plugins.ErrorPlugin;
import org.kendar.http.plugins.GlobalPlugin;
import org.kendar.http.plugins.MockPlugin;
import org.kendar.http.plugins.RecordingPlugin;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HttpProtocol extends CommonProtocol {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocol.class);

    private static HttpsServer createHttpsServer(CertificatesManager certificatesManager, InetSocketAddress sslAddress, int backlog, String cname, String der, String key) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, List.of(), cname, der, key);
        return httpsServer;
    }

    private static SimpleRewriterConfig loadRewritersConfiguration(String key, Ini ini) {
        var proxyConfig = new SimpleRewriterConfig();
        for (var id = 0; id < 255; id++) {
            var when = ini.getValue(key + "-rewriter", "rewrite." + id + ".when", String.class);
            var where = ini.getValue(key + "-rewriter", "rewrite." + id + ".where", String.class);
            var test = ini.getValue(key + "-rewriter", "rewrite." + id + ".test", String.class);
            if (when == null || where == null) {
                continue;
            }
            var remoteServerStatus = new RemoteServerStatus(id + "",
                    when,
                    where,
                    test);
            if (test == null || test.isEmpty()) {
                remoteServerStatus.setRunning(true);
                remoteServerStatus.setForce(true);
            } else {

                remoteServerStatus.setRunning(false);
                remoteServerStatus.setForce(false);
            }
            proxyConfig.getProxies().add(remoteServerStatus);
        }
        return proxyConfig;
    }

    private static boolean notGoodFilter(String sectionKey, Ini ini, PluginDescriptor filter) {
        return !filter.getId().equalsIgnoreCase("global") &&
                !ini.getValue(sectionKey + "-" + filter.getId(), "active", Boolean.class, false);
    }

    @Override
    public void run(String[] args, boolean isExecute, Ini go, Options options) throws Exception {
        options.addOption(createOpt("ht","http", true, "Http port (def 4080)"));
        options.addOption(createOpt("hs","https", true, "Https port (def 4443)"));
        options.addOption(createOpt("prx","proxy", true, "Http/s proxy port (def 9999)"));
        options.addOption(createOpt("ap","apis", true, "The base url for special TPM controllers (def specialApisRoot)"));
        options.addOption(createOpt("prp", "replay", false, "Replay from log/replay source."));
        options.addOption(createOpt("prc", "record", false, "Record to log/replay source."));
        options.addOption(createOpt("plid","replayid", true, "Set an id for the replay instance (default to timestamp_uuid)."));
        options.addOption(createOpt("ae","allowExternal", false, "Allow external calls during replay ."));

        options.addOption(createOpt("cn","cname", true, "Root cname"));
        options.addOption("der", true, "Root certificate");
        options.addOption("key", true, "Root certificate keys");

        options.addOption(createOpt("be","blockExternal", false, "Set if should block external sites replaying"));

        options.addOption("showError", true, "The error to show (404/500 etc) default 0/none");
        options.addOption("errorPercent", true, "The error percent to generate (default 0)");
        options.addOption("errorMessage", true, "The error message");
        if (!isExecute) return;
        setCommonData(args, options, go);
    }

    protected void setCommonData(String[] args, Options options, Ini ini) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        var section = cmd.getOptionValue("protocol");
        ini.putValue(section, "port.http", Integer.parseInt(cmd.getOptionValue("http", "4080")));
        ini.putValue(section, "port.https", Integer.parseInt(cmd.getOptionValue("https", "4443")));
        ini.putValue(section, "port.proxy", Integer.parseInt(cmd.getOptionValue("proxy", "9999")));
        ini.putValue(section, "apis", cmd.getOptionValue("apis", "specialApisRoot"));
        ini.putValue(section, "ssl.cname", cmd.getOptionValue("cname", "C=US,O=Local Development,CN=local.org"));
        ini.putValue(section, "ssl.der", cmd.getOptionValue("der", "resource://certificates/ca.der"));
        ini.putValue(section, "ssl.key", cmd.getOptionValue("key", "resource://certificates/ca.key"));


        if (cmd.hasOption("replay")) {
            ini.putValue(section, "replay", true);
            ini.putValue(section, "replay.respectcallduration", cmd.hasOption("cdt"));
            ini.putValue(section, "replay.replayid", cmd.getOptionValue("replayid", UUID.randomUUID().toString()));
            ini.putValue(section, "replay.blockExternal", !cmd.hasOption("allowExternal"));
        }else if (cmd.hasOption("record")) {
            ini.putValue(section, "record", true);
        }
        if(cmd.hasOption("showError")&&cmd.hasOption("errorPercent")) {
            ini.putValue(section, "error.showError", Integer.parseInt(cmd.getOptionValue("showError","0")));
            ini.putValue(section, "error.errorPercent", Integer.parseInt(cmd.getOptionValue("errorPercent","0")));
            ini.putValue(section, "error.errorMessage", cmd.getOptionValue("errorMessage","Error"));
        }
    }

    @Override
    public String getDefaultPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer, String sectionKey, Ini ini, String protocol,
                      StorageRepository storage, ArrayList<PluginDescriptor> filters, Supplier<Boolean> stopWhenFalseAction) {
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

            var port = ini.getValue(sectionKey, "port.http", Integer.class, 4080);
            var httpsPort = ini.getValue(sectionKey, "port.https", Integer.class, 4443);
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


            var httpServer = HttpServer.create(address, backlog);
            log.debug("Http created");
            ps.setStop(() -> {
                waiterBlock.set(false);
                httpServer.stop(0);
            });

            var der = ini.getValue(sectionKey, "ssl.der", String.class, "resource://certificates/ca.der");
            var key = ini.getValue(sectionKey, "ssl.key", String.class, "resource://certificates/ca.key");
            var cname = ini.getValue(sectionKey, "ssl.cname", String.class, "C=US,O=Local Development,CN=local.org");

            var certificatesManager = new CertificatesManager(new FileResourcesUtils());
            var httpsServer = createHttpsServer(certificatesManager, sslAddress, backlog, cname, der, key);
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
                proxy.terminate();
                httpsServer.stop(0);
                httpServer.stop(0);
            });
            log.debug("Proxy created");
            new Thread(proxy).start();

            var globalFilter = new GlobalPlugin();

            filters.add(globalFilter);
            var global = ini.getSection("global");
            for (var i = filters.size() - 1; i >= 0; i--) {
                var filter = filters.get(i);
                var section = ini.getSection(sectionKey + "-" + filter.getId());
                if (notGoodFilter(sectionKey, ini, filter) && !(filter instanceof AlwaysActivePlugin)) {
                    filters.remove(i);
                    continue;
                }
                //log.info("EXTENSION: " + filter.getId());
                filter.initialize(section,global);
            }

            var httpSection = ini.getSection(sectionKey);
            var rpl = new RecordingPlugin(httpSection);
            if(rpl.isActive())filters.add(rpl.initialize(httpSection,global));

            var ep = new ErrorPlugin(httpSection);
            if(ep.isActive())filters.add(ep.initialize(httpSection,global));


            var mp = new MockPlugin(httpSection);
            if(mp.isActive())filters.add(mp.initialize(httpSection,global));

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
                log.error(ex.getMessage(),ex);
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
}
