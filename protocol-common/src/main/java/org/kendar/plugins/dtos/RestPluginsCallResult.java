package org.kendar.plugins.dtos;

public class RestPluginsCallResult {
    private boolean result;
    private String message;

    public boolean isBlocking() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
