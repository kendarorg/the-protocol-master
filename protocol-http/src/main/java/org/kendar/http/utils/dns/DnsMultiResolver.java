package org.kendar.http.utils.dns;

import java.util.HashMap;
import java.util.List;

public interface DnsMultiResolver {
    void noResponseCaching();

    void clearCache();

    List<String> resolve(String dnsName);

    List<String> resolveLocal(String dnsName);

    List<String> resolveRemote(String dnsName);

    HashMap<String, String> listDomains();
}
