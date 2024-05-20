package org.kendar.redis.parser;

public class RespVerbatimString {
    private String type = "txt";
    private String msg;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
