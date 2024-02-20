package org.kendar.mysql.fsm;

import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.mysql.messages.OkPacket;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;


public class ComPing extends ProtoState {
    public ComPing(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {
        return event.getCommandType() == CommandType.COM_PING;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        return iteratorOfList(new OkPacket());
    }
}
