package org.kendar.command;

import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.kendar.http.HttpProtocol;
import org.kendar.http.plugins.HttpErrorPluginSettings;
import org.kendar.http.plugins.HttpRecordPluginSettings;
import org.kendar.http.plugins.HttpReplayPluginSettings;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.http.settings.HttpSSLSettings;
import org.kendar.http.utils.rewriter.RemoteServerStatus;
import org.kendar.http.utils.rewriter.SimpleRewriterConfig;
import org.kendar.http.utils.ssl.CertificatesManager;
import org.kendar.plugins.PluginDescriptor;
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
import java.util.function.Supplier;

public class HttpRunner extends CommonRunner {
    private static final Logger log = LoggerFactory.getLogger(HttpRunner.class);
    private TcpServer ps;

    private static HttpsServer createHttpsServer(CertificatesManager certificatesManager,
                                                 InetSocketAddress sslAddress, int backlog, String cname, String der,
                                                 String key, List<String> hosts) throws Exception {
        var httpsServer = new KendarHttpsServer(sslAddress, backlog);

        certificatesManager.setupSll(httpsServer, hosts, cname, der, key);
        return httpsServer;
    }


    private static SimpleRewriterConfig loadRewritersConfiguration(HttpProtocolSettings settings) {
        var proxyConfig = new SimpleRewriterConfig();
        for (var i = 0; i < settings.getRewrites().size(); i++) {
            var rw = settings.getRewrites().get(i);
            if (rw.getWhen() == null || rw.getThen() == null) {
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
        ini.getProtocols().put(getId(), section);
        section.setProtocol(getId());
        section.setProtocolInstanceId(getId());
        section.setHttp(Integer.parseInt(cmd.getOptionValue("http", "4080")));
        section.setHttps(Integer.parseInt(cmd.getOptionValue("https", "4443")));
        section.setProxy(Integer.parseInt(cmd.getOptionValue("proxy", "9999")));
        var sslSettings = new HttpSSLSettings();
        sslSettings.setCname(cmd.getOptionValue("cname", "C=US,O=Local Development,CN=local.org"));
        sslSettings.setDer(cmd.getOptionValue("der", "resource://certificates/ca.der"));
        sslSettings.setKey(cmd.getOptionValue("key", "resource://certificates/ca.key"));
        section.setSSL(sslSettings);

        if (cmd.hasOption("replay")) {
            var pl = new HttpReplayPluginSettings();
            pl.setPlugin("replay-plugin");
            pl.setActive(true);
            pl.setRespectCallDuration(cmd.hasOption("cdt"));
            pl.setReplayId(cmd.getOptionValue("replayid", UUID.randomUUID().toString()));
            pl.setBlockExternal(!cmd.hasOption("allowExternal"));
            section.getPlugins().put("replay-plugin", pl);
        } else if (cmd.hasOption("record")) {
            var pl = new HttpRecordPluginSettings();
            pl.setPlugin("record-plugin");
            pl.setActive(true);
            section.getPlugins().put("record-plugin", pl);
        }
        if (cmd.hasOption("showError") && cmd.hasOption("errorPercent")) {
            var pl = new HttpErrorPluginSettings();
            pl.setActive(true);
            pl.setShowError(Integer.parseInt(cmd.getOptionValue("showError", "0")));
            pl.setErrorPercent(Integer.parseInt(cmd.getOptionValue("errorPercent", "0")));
            pl.setErrorMessage(cmd.getOptionValue("errorMessage", "Error"));
        }
    }

    @Override
    public String getDefaultPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void start(ConcurrentHashMap<String, TcpServer> protocolServer,
                      String sectionKey, GlobalSettings ini, ProtocolSettings pset, StorageRepository storage,
                      List<PluginDescriptor> plugins,
                      Supplier<Boolean> stopWhenFalseAction) throws Exception {
        var settings = (HttpProtocolSettings) pset;
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            plugin.initialize(ini, pset);
        }
        var baseProtocol = new HttpProtocol(ini, settings, plugins);
        baseProtocol.initialize();
        ps = new TcpServer(baseProtocol);
        ps.start();
        Sleeper.sleep(5000, () -> ps.isRunning());
        protocolServer.put(sectionKey, ps);
    }


    @Override
    public String getId() {
        return "http";
    }

    @Override
    public Class<?> getSettingsClass() {
        return HttpProtocolSettings.class;
    }

    @Override
    public void stop() {
        ps.stop();
    }
}
