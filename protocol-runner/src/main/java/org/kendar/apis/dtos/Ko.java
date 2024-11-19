package org.kendar.apis.dtos;

public class Ko {
    private String error;

    public Ko(String error) {
        this.error = error;
    }

    public Ko() {
        error = "Generic Error";
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
