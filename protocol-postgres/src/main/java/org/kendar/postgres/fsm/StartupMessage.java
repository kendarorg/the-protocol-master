package org.kendar.postgres.fsm;

import org.kendar.buffers.BBufferUtils;
import org.kendar.dtos.ProcessId;
import org.kendar.postgres.messages.AuthenticationOk;
import org.kendar.postgres.messages.BackendKeyData;
import org.kendar.postgres.messages.ParameterStatus;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public class StartupMessage extends ProtoState {
    private static final int FIXED_SECRET = 5678;
    private static final byte[] STARTUP_MESSAGE_MARKER = BBufferUtils.toByteArray(0x00, 0x03, 0x00, 0x00);

    public StartupMessage(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        if (inputBuffer.size() == 0) return false;
        var hasStartup = inputBuffer.contains(STARTUP_MESSAGE_MARKER, 4);
        var length = inputBuffer.getInt(0);
        return hasStartup && inputBuffer.size() == length;

    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var protoContext = event.getContext();
        var postgresContext = (PostgresProtoContext) protoContext;
        var pid = (ProcessId) protoContext.getValue("PG_PID");
        if (pid == null) {
            pid = new ProcessId(postgresContext.getNewPid());
            protoContext.setValue("PG_PID", pid);
        }
        var pidValue = pid.getPid();
        var length = inputBuffer.getInt(0);

        inputBuffer.truncate(length);
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
                new ReadyForQuery(protoContext.isTransaction()));
    }


}
