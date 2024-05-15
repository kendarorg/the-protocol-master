package org.kendar.redis.parser;

public class Resp3ParseException extends Exception{
    public boolean isMissingData() {
        return missingData;
    }

    private final boolean missingData;

    public Resp3ParseException(String msg){
        super(msg);
        this.missingData = false;
    }
    public Resp3ParseException(String msg, boolean missingData){
        super(msg);
        this.missingData = missingData;
    }
}
