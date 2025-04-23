package org.kendar.mysql.fsm;

import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;

public class ComQuery extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(ComQuery.class);

    public ComQuery(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {

        return event.getCommandType() == CommandType.COM_QUERY;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var inputBuffer = event.getBuffer();
        var context = (MySQLProtoContext) event.getContext();
        var capabilities = context.getClientCapabilities();
//        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {
//
//        }
        var query = inputBuffer.getString(5);
        log.info("[SERVER][QUERY][2]: {}", query);
        var executor = context.getExecutor();
        return executor.executeText(context, query, new ArrayList<>(), true);

    }
}
