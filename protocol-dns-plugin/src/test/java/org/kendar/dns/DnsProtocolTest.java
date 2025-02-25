package org.kendar.dns;

import org.junit.jupiter.api.Test;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.PluginsLoggerFactory;
import org.kendar.utils.Sleeper;
import org.xbill.DNS.*;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DnsProtocolTest {

    public static final int PORT = 5454;

    private static String resolve(String host, int port,String requestedDomain) throws TextParseException {
        var resolver = new SimpleResolver(new InetSocketAddress(host, port));
        var resolvers = new ArrayList<Resolver>();
        resolvers.add(resolver);
        ExtendedResolver extendedResolver = new ExtendedResolver(resolvers);
        extendedResolver.setTimeout(Duration.ofSeconds(1));
        extendedResolver.setRetries(2);
        Lookup lookup = new Lookup(requestedDomain, Type.A);
        lookup.setResolver(extendedResolver);
        lookup.setCache(null);
        lookup.setHostsFileParser(null);
        var records = lookup.run();
        if (records != null) {
            for (org.xbill.DNS.Record record : records) {
                return ((ARecord) records[0]).getAddress().getHostAddress();
            }
        }
        return null;
    }

    @Test
    void roundTripTest() throws UnknownHostException, TextParseException {
        var settings = new DnsProtocolSettings();
        settings.setPort(53);
        settings.setChildDns(List.of("127.0.0.1"));
        var target = new DnsProtocol(new GlobalSettings(),settings, List.of(),new PluginsLoggerFactory());
        target.start();
        Sleeper.sleep(200);

        var data = resolve("127.0.0.1",53,"www.google.com");
        target.terminate();
        assertNull(data);
    }

    @Test
    void localRetrieval() throws UnknownHostException, TextParseException {
        var settings = new DnsProtocolSettings();
        settings.setPort(53);
        settings.setRegistered(List.of(new DnsMapping("10.0.0.1","www.google.com")));
        var target = new DnsProtocol(new GlobalSettings(),settings, List.of(),new PluginsLoggerFactory());
        target.start();
        Sleeper.sleep(200);

        var data = resolve("127.0.0.1",53,"www.google.com");
        target.terminate();
        assertEquals("10.0.0.1", data);
    }

    @Test
    void blockedRetrieval() throws UnknownHostException, TextParseException {
        var settings = new DnsProtocolSettings();
        settings.setPort(53);
        settings.setBlocked(List.of("www.google.com"));
        var target = new DnsProtocol(new GlobalSettings(),settings, List.of(),new PluginsLoggerFactory());
        target.start();
        Sleeper.sleep(200);

        var data = resolve("127.0.0.1",53,"www.google.com");
        target.terminate();
        assertNull( data);
    }


    @Test
    void external() throws UnknownHostException, TextParseException {
        var settings = new DnsProtocolSettings();
        settings.setPort(53);
        settings.setChildDns(List.of("8.8.8.8"));
        var target = new DnsProtocol(new GlobalSettings(),settings, List.of(),new PluginsLoggerFactory());
        target.start();
        Sleeper.sleep(200);

        var data = resolve("127.0.0.1",53,"www.google.com");
        target.terminate();
        assertNotNull( data);
    }
}
