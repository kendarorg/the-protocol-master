package org.kendar.dns;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.ArrayList;
import java.util.List;

@Extension
@TpmService(tags = "dns")
public class DnsProtocolSettings extends ProtocolSettings implements ExtensionPoint {
    private int port = 53;
    private List<String> childDns = new ArrayList<>();
    private List<String> blocked = new ArrayList<>();
    private List<DnsMapping> registered = new ArrayList<>();
    private boolean useCache = false;

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public List<DnsMapping> getRegistered() {
        return registered;
    }

    public void setRegistered(List<DnsMapping> registered) {
        this.registered = registered;
    }

    public List<String> getBlocked() {
        return blocked;
    }

    public void setBlocked(List<String> blocked) {
        this.blocked = blocked;
    }

    public List<String> getChildDns() {
        return childDns;
    }

    public void setChildDns(List<String> childDns) {
        this.childDns = childDns;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
