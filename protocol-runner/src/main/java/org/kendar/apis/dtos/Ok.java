package org.kendar.apis.dtos;

public class Ok {
    private String result = "OK";

    public Ok(String result) {
        this.result = result;
    }
    public Ok(){}
    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
