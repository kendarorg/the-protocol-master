package org.kendar.dns;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.settings.GlobalSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Extension
@TpmService(tags = "dns")
public class DnsProtocol extends NetworkProtoDescriptor implements ExtensionPoint {
    private static final Logger log = LoggerFactory.getLogger(DnsProtocol.class);
    private  DnsProtocolSettings settings;
    private List<String> dnsServers = new ArrayList<>();
    private  List<BasePluginDescriptor> plugins;
    private  ExecutorService executorService = Executors.newFixedThreadPool(20);
    private  Map<String, List<String>> cached = new ConcurrentHashMap<>();
    private  Pattern ipPattern =
            Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private boolean dnsRunning;

    @TpmConstructor
    public DnsProtocol(GlobalSettings ini, DnsProtocolSettings settings,
                       @TpmNamed(tags = "dns") List<BasePluginDescriptor> plugins) {
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
        for (var childDns : settings.getChildDns()) {
            Matcher ipPatternMatcher = ipPattern.matcher(childDns);
            if (!ipPatternMatcher.matches()) {
                try {
                    var chilDnsIp = InetAddress.getByName(childDns);
                    this.dnsServers.add(chilDnsIp.getHostAddress());
                } catch (UnknownHostException e) {
                    log.error("Unable to resolve IP address for DNS {}", childDns, e);
                }
            }else{
                this.dnsServers.add(childDns);
            }
        }
        this.plugins = plugins;
    }

    public static int countOccurrencesOf(String string, String substring) {
        if (string == null || string.length() == 0
                || substring == null || substring.length() == 0) {
            return 0;
        }

        int count = 0;
        int idx;
        for (int pos = 0; (idx = string.indexOf(substring, pos)) != -1; pos = idx + substring.length()) {
            ++count;
        }

        return count;
    }

    @Override
    public List<BasePluginDescriptor> getPlugins() {
        return plugins;
    }

    @Override
    public boolean isBe() {
        return false;
    }

    @Override
    public int getPort() {
        return 53;
    }

    @Override
    protected void initializeProtocol() {

    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId) {
        return null;
    }

    @Override
    public void start() {
        try {
            int port = settings.getPort();
            dnsRunning = true;
            var th = new Thread(() -> {
                runTcp();
            });
            th.start();
            th = new Thread(() -> {
                runUdp();
            });
            th.start();
        } catch (Exception e) {

        }
    }

    byte[] buildErrorMessage(Header header, int rcode, org.xbill.DNS.Record question) {
        Message response = new Message();
        response.setHeader(header);
        for (int i = 0; i < 4; i++) {
            response.removeAllRecords(i);
        }
        if (rcode == Rcode.SERVFAIL) {
            response.addRecord(question, Section.QUESTION);
        }
        header.setRcode(rcode);
        return response.toWire();
    }

    public byte[] errorMessage(Message query, int rcode) {
        return buildErrorMessage(query.getHeader(), rcode, query.getQuestion());
    }

    private byte[] buildResponse(byte[] in) throws IOException {
        Message request;
        request = new Message(in);
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        //response.getHeader().setFlag(Flags.AA );

        String requestedDomain = request.getQuestion().getName().toString(true);

        log.debug("Requested domain " + requestedDomain);

        List<String> ips = new ArrayList<>();

        var splitted = requestedDomain.split("\\.");
        var containsAtLeastOneInternal = false;
        var endsWith = false;
        var isUpperCase = requestedDomain.toUpperCase(Locale.ROOT).equals(requestedDomain);
        if (splitted.length >= 3) {
            var occurr = (splitted[splitted.length - 2] + "." + splitted[splitted.length - 1]).toLowerCase(Locale.ROOT);
            containsAtLeastOneInternal = countOccurrencesOf(requestedDomain.toLowerCase(Locale.ROOT), "." + occurr + ".") >= 1;
            endsWith = requestedDomain.toLowerCase(Locale.ROOT).endsWith("." + occurr);
        }
        response.addRecord(request.getQuestion(), Section.QUESTION);


        if (!(containsAtLeastOneInternal && endsWith) && !isUpperCase) {
            if (settings.getBlocked().stream().noneMatch(bl -> bl.equalsIgnoreCase(requestedDomain))) {
                ips = doResolve(requestedDomain);
            }
        }
        if (requestedDomain.equalsIgnoreCase("1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa")) {
            ips = new ArrayList<>();
            ips.add("127.0.0.1");
        }

        byte[] resp;
        if (ips.size() > 0) {
            for (String ip : ips) {
                log.debug("FOUNDED IP " + ip + " FOR " + requestedDomain);
                // Add answers as needed
                response.addRecord(
                        org.xbill.DNS.Record.fromString(Name.fromString(requestedDomain + "."), Type.A, DClass.IN, 1000, ip,
                                Name.empty),
                        Section.ANSWER);
            }
            resp = response.toWire();
        } else {
            resp = errorMessage(request, Rcode.NXDOMAIN);
        }
        return resp;
    }

    private void resolveAll(OutputStream outputStream, byte[] in) {
        try {
            byte[] resp = buildResponse(in);
            var dataOutputStream = new DataOutputStream(outputStream);
            short length = (short) resp.length;
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort(length);
            dataOutputStream.write(buffer.array());
            dataOutputStream.write(resp);
            dataOutputStream.flush();
            log.debug("SENDING RESPONSE");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static final int UDP_SIZE = 512;

    private void resolveAll(InetAddress inAddress, int inPort, DatagramSocket socket, byte[] in) {
        try {
            // Build the response
            byte[] resp = buildResponse(in);
            log.debug("SENDING RESPONSE UDP");
            DatagramPacket outdp =
                    new DatagramPacket(resp, resp.length, inAddress, inPort);
            socket.send(outdp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runUdp() {
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("0.0.0.0", settings.getPort()));
            // socket.setSoTimeout(0);
            log.info("Dns server LOADED, port: " + settings.getPort());
            byte[] in = new byte[UDP_SIZE];

            // Read the request
            DatagramPacket indp = new DatagramPacket(in, UDP_SIZE);

            while (dnsRunning) {
                indp.setLength(in.length);

                try {
                    socket.receive(indp);
                    var inCopy = in.clone();
                    var inAddress = indp.getAddress();
                    var inPort = indp.getPort();
                    executorService.submit(() -> resolveAll(inAddress, inPort, socket, inCopy));

                } catch (InterruptedIOException e) {

                }
            }
        } catch (Exception ex) {
            log.error("Error running udp thread", ex);
        }
    }

    private void runTcp() {

        try (ServerSocket serverSocket = new ServerSocket(settings.getPort())) {
            while (dnsRunning) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> {

                    try {
                        var in = clientSocket.getInputStream();
                        DataInputStream dis = new DataInputStream(in);

                        int len = dis.readUnsignedShort();
                        byte[] data = new byte[len];
                        if (len > 0) {
                            dis.readFully(data);
                        }
                        resolveAll(clientSocket.getOutputStream(), data);
                        clientSocket.close();

                    } catch (IOException e) {
                        log.error("ERror reading from DNS tcp stream", e);
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Error running tcp thread", ex);
        }
    }

    @Override
    public boolean isWrapperRunning() {
        return dnsRunning;
    }

    @Override
    public boolean isWrapper() {
        return true;
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
        dnsRunning = false;
    }

    public List<String> doResolve(String requestedDomain) {
        if (requestedDomain == null || requestedDomain.length() == 0) {
            return new ArrayList<>();
        }
        var matching = settings.getRegistered().stream().filter(d -> d.getName().equalsIgnoreCase(requestedDomain)).findFirst();
        if (matching.isPresent()) {
            return List.of(matching.get().getIp());
        }
        if (settings.isUseCache()) {
            var item = cached.get(requestedDomain.toLowerCase());
            if (item != null) {
                return new ArrayList<>(item);
            }
        }
        var result = resolveRemote(requestedDomain);
        if (settings.isUseCache()) {
            cached.put(requestedDomain.toLowerCase(), result);
        }
        return result;
    }

    public List<String> resolveRemote(String requestedDomain) {
        Matcher ipPatternMatcher = ipPattern.matcher(requestedDomain);
        if (ipPatternMatcher.matches()) {
            return List.of(requestedDomain);
        }
        if (requestedDomain.toUpperCase().equalsIgnoreCase("localhost".toUpperCase(Locale.ROOT))) {
            return List.of("127.0.0.1");
        } else if (requestedDomain.toUpperCase().endsWith("in-addr.arpa".toUpperCase(Locale.ROOT))) {
            return List.of("127.0.0.1");
        } else if (requestedDomain.toUpperCase().endsWith("ip6.arpa".toUpperCase(Locale.ROOT))) {
            return List.of("127.0.0.1");
        }

        List<Callable<List<String>>> runnables = new ArrayList<>();
        var extraServersList = settings.getChildDns();
        for (int i = 0; i < extraServersList.size(); i++) {
            var serverToCall = extraServersList.get(i);
            var runnable = new DnsRunnable(serverToCall, requestedDomain);
            runnables.add(runnable);
        }
        var data = new HashSet<String>();
        List<Future<List<String>>> futures = new ArrayList<>();
        try {
            futures = executorService.invokeAll(runnables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int finished = futures.size();
        // This method returns the time in millis
        long timeMilli = new Date().getTime();
        long timeEnd = timeMilli + 2000;

        while (finished != 0) {
            if (timeEnd <= new Date().getTime()) {
                // System.out.println("================");
                for (var current : futures) {
                    current.cancel(true);
                }
                break;
            }
            finished = futures.size();
            for (var current : futures) {
                if (current.isCancelled()) {
                    finished--;
                } else if (current.isDone()) {
                    finished--;
                    try {
                        var currentData = current.get();
                        if (currentData.size() == 0) {
                            continue;
                        }
                        data.addAll(current.get());
                        for (var future : futures) {
                            if (!future.isDone()) {
                                future.cancel(true);
                            }
                        }
                        futures.clear();
                        if (data.size() > 0) {
                            //localDomains.put(requestedDomain.toLowerCase(Locale.ROOT), new HashSet<>(data));
                            finished = 0;
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("Unable to try resolve " + requestedDomain);
                    }
                }
            }
        }
        var result = new ArrayList<>(data);
        if(result.size()==0){
            return List.of();
        }
        log.debug("Resolved remote " + requestedDomain + "=>" + result.get(0));


        return result;
    }
}
