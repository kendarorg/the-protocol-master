package org.kendar.http.plugins.settings;

import java.util.ArrayList;
import java.util.List;

public class HttpSSLSettings {
    private String cname = "C=US,O=Local Development,CN=local.org";
    private String der = "resource://certificates/ca.der";
    private String key = "resource://certificates/ca.key";
    private List<String> hosts = new ArrayList<>();

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getDer() {
        return der;
    }

    public void setDer(String der) {
        this.der = der;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
