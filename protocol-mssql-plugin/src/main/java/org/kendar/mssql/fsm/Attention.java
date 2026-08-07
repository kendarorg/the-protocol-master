package org.kendar.mssql.fsm;

import org.kendar.mssql.constants.DoneStatus;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.mssql.messages.DoneToken;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Attention (cancel) request: acknowledge with a DONE carrying the
 * DONE_ATTN status after trying to cancel the running statement
 */
public class Attention extends ProtoState implements InterruptProtoState {
    private static final Logger log = LoggerFactory.getLogger(Attention.class);

    public Attention(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(TdsPacket event) {
        return event.getPacketType() == TdsPacketType.ATTENTION;
    }

    public Iterator<ProtoStep> execute(TdsPacket event) {
        var protoContext = (MssqlProtoContext) event.getContext();
        log.debug("[SERVER][ATTENTION] Cancel requested");
        protoContext.cancel();
        return iteratorOfList(protoContext.newMessage()
                .add(new DoneToken(DoneStatus.DONE_ATTN, 0)));
    }
}
