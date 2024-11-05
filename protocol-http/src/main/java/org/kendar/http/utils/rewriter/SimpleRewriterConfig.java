package org.kendar.http.utils.rewriter;

import java.util.ArrayList;
import java.util.List;

public class SimpleRewriterConfig {
    private List<RemoteServerStatus> proxies = new ArrayList<>();
    private String id;
    private boolean system;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SimpleRewriterConfig copy() {
        var result = new SimpleRewriterConfig();
        result.setProxies(new ArrayList<>());
        for (var rss : proxies) {
            result.getProxies().add(rss.copy());
        }
        result.setId(this.getId());
        result.setSystem(this.isSystem());
        return result;
    }

    public List<RemoteServerStatus> getProxies() {
        return proxies;
    }

    public void setProxies(List<RemoteServerStatus> proxies) {
        for (var proxy : proxies) {
            proxy.setRunning(false);
        }
        this.proxies = proxies;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}
