package org.kendar.http.settings;

public class HttpRewriteSettings {
    private String when;
    private String then;
    private String test;
    private boolean forceActive;

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public String getThen() {
        return then;
    }

    public void setThen(String then) {
        this.then = then;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public boolean isForceActive() {
        return forceActive;
    }

    public void setForceActive(boolean forceActive) {
        this.forceActive = forceActive;
    }
}
