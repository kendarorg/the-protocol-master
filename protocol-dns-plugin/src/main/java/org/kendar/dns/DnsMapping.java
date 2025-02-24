package org.kendar.dns;

public class DnsMapping {
    private String ip;
    private String name;
    private long timestamp;

    public DnsMapping(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    public DnsMapping() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
