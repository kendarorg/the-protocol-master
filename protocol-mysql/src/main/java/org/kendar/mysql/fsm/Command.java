package org.kendar.mysql.fsm;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class Command extends MySQLProtoState {
    public Command(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected boolean canRunBytes(BytesEvent event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeBytes(MySQLBBuffer inputBuffer, BytesEvent event, int packetLength, int packetIndex) {
        var tt = inputBuffer.get();
        var commandType = CommandType.of(tt);
        var newBuffer = (MySQLBBuffer) event.getContext().buildBuffer();
        newBuffer.write(inputBuffer.getBytes(0, packetLength + 4));
        newBuffer.setPosition(5);
        var commandEvent = new CommandEvent(event.getContext(), CommandType.class, newBuffer, commandType);
        event.getContext().send(commandEvent);
        return iteratorOfEmpty();
    }
}
