package org.kendar.plugins.apis;

public class Ok {
    private String result = "OK";

    public Ok(String result) {
        this.result = result;
    }

    public Ok() {
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
