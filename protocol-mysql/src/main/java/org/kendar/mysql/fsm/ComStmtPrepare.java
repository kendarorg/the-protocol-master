package org.kendar.mysql.fsm;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.executor.MySQLExecutor;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class ComStmtPrepare extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(ComStmtPrepare.class);

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
//        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {
//
//        }
        var query = inputBuffer.getString(5);

        log.debug("[SERVER][STMTPREP]: {}", query);
        var executor = new MySQLExecutor();
        return executor.prepareStatement(context, query);
    }
}
