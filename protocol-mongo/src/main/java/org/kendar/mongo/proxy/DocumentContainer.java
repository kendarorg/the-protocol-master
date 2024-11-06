package org.kendar.mongo.proxy;

import org.bson.Document;

public class DocumentContainer {
    private final int reqResId;
    private final int requestId;
    private Document commandResult;

    public DocumentContainer(int reqResId, int requestId) {

        this.reqResId = reqResId;
        this.requestId = requestId;
    }

    public int getReqResId() {
        return reqResId;
    }

    public int getRequestId() {
        return requestId;
    }

    public Document getCommandResult() {
        return commandResult;
    }

    public void setCommandResult(Document commandResult) {
        this.commandResult = commandResult;
    }

    public void setResult(Document commandResult) {

        this.commandResult = commandResult;
    }
}
