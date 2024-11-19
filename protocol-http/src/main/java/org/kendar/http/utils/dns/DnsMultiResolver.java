package org.kendar.http.utils.dns;

import java.util.List;
import java.util.Map;

public interface DnsMultiResolver {
    Map<String, String> listDomains();

    void clearCache();

    List<String> resolve(String dnsName);

    List<String> resolveRemote(String dnsName);
}
