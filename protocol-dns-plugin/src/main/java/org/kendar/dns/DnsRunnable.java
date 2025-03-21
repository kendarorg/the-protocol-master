package org.kendar.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class DnsRunnable implements Callable<List<String>> {
    private static final Logger log = LoggerFactory.getLogger(DnsRunnable.class);
    private final String requestedServer;
    private final String requestedDomain;

    public DnsRunnable(String requestedServer, String requestedDomain) {
        this.requestedServer = requestedServer;
        this.requestedDomain = requestedDomain;
    }

    @Override
    public List<String> call() throws Exception {
        SimpleResolver resolver;
        List<String> result = new ArrayList<>();
        try {
            log.debug("Request to {} for {}", requestedServer, requestedDomain);
            resolver = new SimpleResolver(requestedServer);
            resolver.setPort(53);
            var resolvers = new ArrayList<Resolver>();
            resolvers.add(resolver);
            ExtendedResolver extendedResolver = new ExtendedResolver(resolvers);
            extendedResolver.setTimeout(Duration.ofSeconds(1));
            extendedResolver.setRetries(2);
            Lookup lookup = new Lookup(requestedDomain.toUpperCase(Locale.ROOT), Type.A);
            lookup.setResolver(extendedResolver);
            lookup.setCache(null);
            lookup.setHostsFileParser(null);
            var records = lookup.run();
            if (records != null) {
                for (org.xbill.DNS.Record record : records) {
                    String realip = ((ARecord) records[0]).getAddress().getHostAddress();
                    log.debug("Resolved with {} for {}: {}", requestedServer, requestedDomain, realip);
                    result.add(realip);

                }
            }
        } catch (UnknownHostException | TextParseException e) {
            throw new UnknownHostException();
        }
        if (result.isEmpty()) {
            log.debug("NOTRESOLVED with {} for {}", requestedServer, requestedDomain);
        }
        return result;
    }
}
