package org.kendar.mysql.fsm;

import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;
import org.kendar.protocol.fsm.Stop;

import java.util.Iterator;

public class ComQuit extends ProtoState {
    public ComQuit(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {
        return event.getCommandType() == CommandType.COM_QUIT;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        return iteratorOfList(new Stop());
        
    }
}
