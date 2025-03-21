package org.kendar.http.utils.dns;

import org.kendar.exceptions.TPMProtocolException;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DnsMultiResolverImpl implements DnsMultiResolver {
    final ConcurrentHashMap<String, String> cachedDomains = new ConcurrentHashMap<>();


    @Override
    public void clearCache() {
        cachedDomains.clear();
    }

    @Override
    public List<String> resolve(String dnsName) {
        try {
            var addresses = cachedDomains.computeIfAbsent(dnsName, k -> {

                try {
                    InetAddress result = null;
                    result = InetAddress.getByName(dnsName);
                    return result.getHostAddress();
                } catch (Exception e) {
                    throw new TPMProtocolException(e);
                }
            });
            if (addresses == null) {
                return List.of();
            }
            return List.of(addresses);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<String> resolveRemote(String dnsName) {
        return resolve(dnsName);
    }

    @Override
    public Map<String, String> listDomains() {
        return new HashMap<>(cachedDomains);
    }
}
