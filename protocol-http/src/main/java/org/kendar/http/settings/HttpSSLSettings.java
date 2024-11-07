package org.kendar.http.settings;

public class HttpSSLSettings {
    private String cname;
    private String der;
    private String key;

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getCname() {
        return cname;
    }

    public void setDer(String der) {
        this.der = der;
    }

    public String getDer() {
        return der;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
