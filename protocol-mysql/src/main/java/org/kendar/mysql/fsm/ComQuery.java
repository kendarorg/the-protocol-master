package org.kendar.mysql.fsm;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.executor.MySQLExecutor;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.ArrayList;
import java.util.Iterator;

public class ComQuery extends ProtoState {
    public ComQuery(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {

        return event.getCommandType() == CommandType.COM_QUERY;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var inputBuffer = (MySQLBBuffer) event.getBuffer();
        var context = (MySQLProtoContext) event.getContext();
        var capabilities = context.getClientCapabilities();
        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {

        }
        var query = inputBuffer.getString(5);
        System.out.println("[SERVER] \tExecuting: " + query);
        var executor = new MySQLExecutor();
        return executor.executeText(context, query, new ArrayList<>(), true);

    }
}
