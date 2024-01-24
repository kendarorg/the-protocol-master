package org.kendar.mysql.fsm;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

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
        return inputBuffer.size() >= packetLength && canRunBytes(event);
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
