package org.kendar;

public class Main {

   /* private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static GlobalFilter globalFilter;
    private static CertificatesManager certificatesManager;

    public static void main(String[] args) throws Exception {
        var shutdown = new AtomicBoolean(false);
        runInternal(args, shutdown);
        new Thread(() -> {
            var fileInputStream = new InputStreamReader(System.in);
            var bufferedReader = new BufferedReader(fileInputStream);
            while (true) {
                try {
                    var data = bufferedReader.readLine();
                    if (data.equalsIgnoreCase("q")) {
                        shutdown.set(true);
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        while (!shutdown.get()) {
            Sleeper.sleep(10);
        }
        globalFilter.terminate();


    }

    private static void runInternal(String[] args, AtomicBoolean shutdown) throws Exception {
//        Options options = new Options();
//        options.addOption("cfg", true, "Load config file");
//
//        CommandLineParser parser = new DefaultParser();
//        CommandLine cmd = parser.parse(options, args);
        String configFile = null;//cmd.getOptionValue("cgf");
        if (configFile == null) {
            configFile = "main.ini";
        }

        log.info("Loading config file: " + configFile);
        var ini = new Ini();
        ini.load(Path.of(configFile).toAbsolutePath().toFile());
        var pluginsDir = ini.getValue("global", "pluginsDir", String.class, "plugins");
        for (var key : ini.getSections()) {
            if (key.equalsIgnoreCase("http")) continue;
        }


        var pluginManager = new JarPluginManager(Path.of(pluginsDir).toAbsolutePath());
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        var port = ini.getValue("http", "http.port", Integer.class, 8085);
        var httpsPort = ini.getValue("http", "https.port", Integer.class, port + 400);
        var proxyPort = ini.getValue("http", "port.proxy", Integer.class, 9999);
        log.info("LISTEN HTTP: " + port);
        log.info("LISTEN HTTPS: " + httpsPort);
        log.info("LISTEN PROXY: " + proxyPort);
        var backlog = 60;
        var useCachedExecutor = true;
        var address = new InetSocketAddress(port);
        var sslAddress = new InetSocketAddress(httpsPort);

        // initialise the HTTP server
        var proxyConfig = loadRewritersConfiguration(ini);
        var dnsHandler = new DnsMultiResolverImpl();
        var connectionBuilder = new ConnectionBuilderImpl(dnsHandler);
        var requestResponseBuilder = new RequestResponseBuilderImpl();


        certificatesManager = new CertificatesManager(new FileResourcesUtils());
        var httpServer = HttpServer.create(address, backlog);

        var der = ini.getValue("ssl", "der", String.class, "resources://certificates/ca.der");
        var key = ini.getValue("ssl", "key", String.class, "resources://certificates/ca.key");
        var cname = ini.getValue("ssl", "cname", String.class,"C=US,O=Local Development,CN=local.org");

        var httpsServer = createHttpsServer(sslAddress, backlog,cname, der, key);


        var proxy = new ProxyServer(proxyPort)
                .withHttpRedirect(port).withHttpsRedirect(httpsPort)
                .withDnsResolver(host -> {
                    try {
                        certificatesManager.setupSll(httpsServer, List.of(host),cname, der, key);
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
        new Thread(proxy).start();
        var filters = new ArrayList<HttpFilterDescriptor>();

        globalFilter = new GlobalFilter();

        filters.add(globalFilter);
        filters.add(new RecordingFilter());
        filters.add(new ErrorFilter());
        filters.add(new MockFilter());
        filters.addAll(pluginManager.getExtensions(HttpFilterDescriptor.class));
        for (var i = filters.size() - 1; i >= 0; i--) {
            var filter = filters.get(i);
            var section = ini.getSection(filter.getId());
            if (!filter.getId().equalsIgnoreCase("http") &&
                    !ini.getValue(filter.getId(), "active", Boolean.class, false)) {
                filters.remove(i);
                continue;
            }
            log.info("EXTENSION: " + filter.getId());
            filter.initialize(section);
        }
        globalFilter.setFilters(filters);
        globalFilter.setServer(httpServer, httpsServer);
        globalFilter.setShutdownVariable(shutdown);
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
    }


    private static HttpsServer createHttpsServer(InetSocketAddress sslAddress, int backlog,String cname, String der, String key) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, List.of(),cname, der, key);
        return httpsServer;
    }


    private static SimpleRewriterConfig loadRewritersConfiguration(Ini ini) {
        var proxyConfig = new SimpleRewriterConfig();
        for (var id = 0; id < 255; id++) {
            var when = ini.getValue("rewriter", "rewrite." + id + ".when", String.class);
            var where = ini.getValue("rewriter", "rewrite." + id + ".where", String.class);
            if (when == null || where == null) {
                continue;
            }
            var remoteServerStatus = new RemoteServerStatus(id + "",
                    when,
                    where,
                    "https://www.google.com");
            remoteServerStatus.setRunning(true);
            remoteServerStatus.setForce(true);
            proxyConfig.getProxies().add(remoteServerStatus);
        }
        return proxyConfig;
    }
*/

}