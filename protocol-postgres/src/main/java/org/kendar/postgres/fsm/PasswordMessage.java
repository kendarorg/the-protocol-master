package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.iterators.ProcessId;
import org.kendar.postgres.messages.AuthenticationOk;
import org.kendar.postgres.messages.BackendKeyData;
import org.kendar.postgres.messages.ParameterStatus;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

import static org.kendar.postgres.fsm.StartupMessage.FIXED_SECRET;

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
        int length = inputBuffer.getInt();
        var password = inputBuffer.getUtf8String(length - 4);
        protoContext.setValue("password", password);

        var pid = (ProcessId) protoContext.getValue("PG_PID");
        if (pid == null) {
            pid = new ProcessId(protoContext.getPid());
            protoContext.setValue("PG_PID", pid);
        }
        var pidValue = pid.getPid();
        return iteratorOfList(
                new AuthenticationOk(),
                new ParameterStatus("server_version", "15"),
                new ParameterStatus("server_type", "JANUS"),
                new ParameterStatus("client_encoding", "UTF8"),
                new ParameterStatus("DateStyle", "ISO, YMD"),
                new ParameterStatus("TimeZone", "CET"),
                new ParameterStatus("is_superuser", "on"),
                new ParameterStatus("integer_datetimes", "on"),
                new BackendKeyData(pidValue, FIXED_SECRET),
                new ReadyForQuery(protoContext.getValue("TRANSACTION", false)));
    }
}
