package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.iterators.ProcessId;
import org.kendar.postgres.messages.AuthenticationOk;
import org.kendar.postgres.messages.BackendKeyData;
import org.kendar.postgres.messages.ParameterStatus;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static org.kendar.postgres.fsm.StartupMessage.FIXED_SECRET;
import static org.kendar.postgres.fsm.StartupMessage.readNullTerminatedStrings;

public class PasswordMessage extends PostgresState {

    public PasswordMessage(Class<?>... messages) {
        super(messages);
    }


    @Override
    protected byte getMessageId() {
        return 'p';
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, NetworkProtoContext context) {
        var protoContext = (PostgresProtoContext) context;
        //var p = inputBuffer.get();
        //var length = inputBuffer.getInt(0);
        inputBuffer.setPosition(5);
        var data =  new String(inputBuffer.getRemaining());
        data = data.substring(0,data.length()-1);
        protoContext.setValue("password", data);

        var pid = (ProcessId) protoContext.getValue("PG_PID");
        if (pid == null) {
            pid = new ProcessId(protoContext.getPid());
            protoContext.setValue("PG_PID", pid);
        }

        var pidValue = pid.getPid();
        var parameterStatus = context.getValue("SERVER_PARAMETERS", new HashMap<String, ParameterStatus>());

        var toSend = new ArrayList<ReturnMessage>();
        toSend.add(new AuthenticationOk());
        for (var ps : parameterStatus.values()) {
            toSend.add(ps);
        }
        toSend.add(new BackendKeyData(pidValue, FIXED_SECRET));
        toSend.add(new ReadyForQuery(protoContext.getValue("TRANSACTION", false)));

        return iteratorOfList(toSend.toArray(new ReturnMessage[]{}));
    }
}
