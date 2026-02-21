package org.kendar.postgres.fsm;

import org.kendar.buffers.BBufferUtils;
import org.kendar.iterators.ProcessId;
import org.kendar.postgres.messages.AuthenticationOk;
import org.kendar.postgres.messages.BackendKeyData;
import org.kendar.postgres.messages.ParameterStatus;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StartupMessage extends ProtoState {
    public static final byte[] STARTUP_MESSAGE_MARKER = BBufferUtils.toByteArray(0x00, 0x03, 0x00, 0x00);
    public static final int FIXED_SECRET = 5678;

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

    public static Map<String,String> readNullTerminatedStrings(byte[] data) {
        var result = new HashMap<String,String>();
        int start = 0;
        String prev = null;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                if (i > start) { // avoid empty trailing \0
                    String s = new String(data, start, i - start, StandardCharsets.UTF_8);
                    if(prev==null){
                        prev = s;
                    } else {
                        result.put(prev, s);
                        prev = null;
                    }
                }
                start = i + 1;
            }
        }

        return result;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var protoContext = event.getContext();
        var postgresContext = (PostgresProtoContext) protoContext;
        var pid = (ProcessId) protoContext.getValue("PG_PID");
        if (pid == null) {
            pid = new ProcessId(postgresContext.getPid());
            protoContext.setValue("PG_PID", pid);
        }
        var pidValue = pid.getPid();
        var length = inputBuffer.getInt(0);
        var data = inputBuffer.getBytes(8, length - 8);
        var dataMap = readNullTerminatedStrings(data);
        var parameterStatus = new HashMap<String,ParameterStatus>();
        parameterStatus.put("server_version",new ParameterStatus("server_version", "15"));
        parameterStatus.put("server_type",new ParameterStatus("server_type", "JANUS"));
        parameterStatus.put("client_encoding",new ParameterStatus("client_encoding", "UTF8"));
        parameterStatus.put("DateStyle",new ParameterStatus("DateStyle", "ISO, YMD"));
        parameterStatus.put("TimeZone",new ParameterStatus("TimeZone", "CET"));
        parameterStatus.put("is_superuser",new ParameterStatus("is_superuser", "on"));
        parameterStatus.put("integer_datetimes",new ParameterStatus("integer_datetimes", "on"));

        for(var dm:dataMap.entrySet()){
            parameterStatus.put(dm.getKey(),new ParameterStatus(dm.getKey(),dm.getValue()));
        }

        inputBuffer.truncate(length);
        postgresContext.setValue("database", dataMap.get("database"));
        postgresContext.setValue("userid", dataMap.get("user"));
        var jdbcSettings = (JdbcProtocolSettings) event.getContext().getDescriptor().getSettings();

        var toSend = new ArrayList<ReturnMessage>();
        if(jdbcSettings.isUseTls()){
            postgresContext.setValue("SERVER_PARAMETERS", parameterStatus);
            return iteratorOfList(new AuthenticationOk(3));
        }else {
            toSend.add(new AuthenticationOk());
        }
        for(var ps:parameterStatus.values()){
            toSend.add(ps);
        }
        toSend.add(new BackendKeyData(pidValue, FIXED_SECRET));
        toSend.add(new ReadyForQuery(protoContext.getValue("TRANSACTION", false)));

        return iteratorOfList(toSend);
    }


}
