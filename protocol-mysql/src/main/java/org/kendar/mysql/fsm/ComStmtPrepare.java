package org.kendar.mysql.fsm;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.executor.MySQLExecutor;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public class ComStmtPrepare extends ProtoState {
    public ComStmtPrepare(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {

        return event.getCommandType() == CommandType.COM_STMT_PREPARE;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var inputBuffer = (MySQLBBuffer) event.getBuffer();
        var context = (MySQLProtoContext) event.getContext();
        var capabilities = context.getClientCapabilities();
        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {

        }
        var query = inputBuffer.getString(5);
        System.out.println("[SERVER] \tPreparing: " + query);
        var executor = new MySQLExecutor();
        return executor.prepareStatement(context, query);
    }
}
