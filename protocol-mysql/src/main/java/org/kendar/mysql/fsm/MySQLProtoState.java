package org.kendar.mysql.fsm;

import org.kendar.exceptions.AskMoreDataException;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public abstract class MySQLProtoState extends ProtoState {
    public MySQLProtoState(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = (MySQLBBuffer) event.getBuffer();
        if (inputBuffer.size() < 3) return false;
        inputBuffer.setPosition(0);
        var packetLength = inputBuffer.readUB3() + 4;
        inputBuffer.setPosition(0);
        if (inputBuffer.size() >= packetLength) {
            return canRunBytes(event);
        } else {
            throw new AskMoreDataException();
        }
    }

    protected abstract boolean canRunBytes(BytesEvent event);

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = (MySQLBBuffer) event.getBuffer();
        var packetLength = inputBuffer.readUB3();
        var packetIndex = (int) inputBuffer.get();
        var result = executeBytes(inputBuffer, event, packetLength, packetIndex);
        inputBuffer.setPosition(packetLength + 4);
        return result;
    }

    protected abstract Iterator<ProtoStep> executeBytes(MySQLBBuffer inputBuffer, BytesEvent event, int packetLength, int packetIndex);
}
