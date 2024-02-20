package org.kendar.postgres.fsm;

import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.Stop;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class CancelRequest extends ProtoState implements InterruptProtoState {
    public CancelRequest(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        if (inputBuffer.size() != 16) {
            return false;
        }
        var marker = inputBuffer.getInt(4);
        return marker == 80877102;
    }


    public Iterator<ProtoStep> execute(BytesEvent event) {
        var context = (PostgresProtoContext) event.getContext();
        context.stop();
        var inputBuffer = event.getBuffer();
        var pid = inputBuffer.getInt(8);
        var secret = inputBuffer.getInt(12);
        var contextToCancel = PostgresProtoContext.getContextByPid(pid);
        var statement = (Statement) contextToCancel.getValue("EXECUTING_NOW");
        if (statement != null) {
            try {
                statement.cancel();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return iteratorOfList(new Stop());
    }

}
