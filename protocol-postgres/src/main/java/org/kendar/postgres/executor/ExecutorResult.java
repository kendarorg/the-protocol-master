package org.kendar.postgres.executor;

import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class ExecutorResult {
    private Iterator<ProtoStep> returnMessages;
    private boolean runNow;

    public ExecutorResult(Iterator<ProtoStep> returnMessages) {
        this.returnMessages = returnMessages;
    }

    public ExecutorResult runNow() {
        runNow = true;
        return this;
    }


    public Iterator<ProtoStep> getReturnMessages() {
        return returnMessages;
    }

    public void setReturnMessages(Iterator<ProtoStep> returnMessages) {
        this.returnMessages = returnMessages;
    }

    public boolean isRunNow() {
        return runNow;
    }

    public void setRunNow(boolean runNow) {
        this.runNow = runNow;
    }
}
