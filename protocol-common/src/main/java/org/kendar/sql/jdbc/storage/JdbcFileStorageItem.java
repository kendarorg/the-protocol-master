package org.kendar.sql.jdbc.storage;

public class JdbcFileStorageItem {
    private JdbcRequest request;
    private JdbcResponse response;
    private long durationMs;
    private String type;

    public JdbcFileStorageItem() {
    }

    public JdbcFileStorageItem(JdbcRequest request, JdbcResponse response, long durationMs, String type) {
        this.request = request;
        this.response = response;
        this.durationMs = durationMs;
        this.type = type;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getType() {
        return type;
    }

    public JdbcRequest getRequest() {
        return request;
    }

    public void setRequest(JdbcRequest request) {
        this.request = request;
    }

    public JdbcResponse getResponse() {
        return response;
    }

    public void setResponse(JdbcResponse response) {
        this.response = response;
    }
}
