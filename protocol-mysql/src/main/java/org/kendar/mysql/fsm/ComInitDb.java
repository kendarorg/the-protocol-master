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

import java.util.ArrayList;
import java.util.Iterator;

public class ComInitDb extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(ComInitDb.class);

    public ComInitDb(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {
        return event.getCommandType() == CommandType.COM_INIT_DB;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var inputBuffer = (MySQLBBuffer) event.getBuffer();
        var context = (MySQLProtoContext) event.getContext();
        var database = inputBuffer.getString(5);
        var executor = new MySQLExecutor();
        return executor.executeText(context, "USE " + database + ";", new ArrayList<>(), true);
    }
}
