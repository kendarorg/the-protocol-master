package org.kendar.mysql.fsm;

import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.Stop;

import java.util.Iterator;

public class ComQuit extends ProtoState {
    public ComQuit(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {
        return event.getCommandType() == CommandType.COM_QUIT;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        return iteratorOfRunner(new Stop());

    }
}
