package org.kendar.mongo.fsm;

import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public abstract class StandardOpMsgCommand extends ProtoState {

    public StandardOpMsgCommand(Class<?>... events) {
        super(events);
    }


    public boolean canRun(OpMsgRequest event) {
        var canRun = false;
        var lsatOp = event.getData();
        for (var section : lsatOp.getSections()) {
            for (var doc : section.getDocuments()) {
                if (canRun(section.getIdentifier(), doc)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract boolean canRun(String identifier, String doc);

    public Iterator<ProtoStep> execute(OpMsgRequest event) {
        var lsatOp = event.getData();
        return executeInternal(event);
    }

    protected abstract Iterator<ProtoStep> executeInternal(OpMsgRequest event);
}
