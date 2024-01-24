package org.kendar.mysql.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.protocol.ReturnMessage;

public abstract class MySQLReturnMessage extends ReturnMessage {
    private int packetNumber;

    public MySQLReturnMessage withPacketNumber(int packetNumber) {
        this.setPacketNumber(packetNumber);
        return this;
    }

    public int getPacketNumber() {
        return packetNumber;
    }

    public void setPacketNumber(int packetNumber) {
        this.packetNumber = packetNumber;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.write(new byte[3]);
        resultBuffer.write((byte) packetNumber);
        writeResponse((MySQLBBuffer) resultBuffer);
        var len = resultBuffer.size() - 4;
        var pos = resultBuffer.getPosition();
        resultBuffer.setPosition(0);
        ((MySQLBBuffer) resultBuffer).writeUB3(len);
        resultBuffer.setPosition(pos);
    }

    protected abstract void writeResponse(MySQLBBuffer resultBuffer);
}
