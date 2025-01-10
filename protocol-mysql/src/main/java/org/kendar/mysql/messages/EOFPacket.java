package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;

public class EOFPacket extends MySQLReturnMessage {

    private int statusFlags;
    private int warnings;

    private int capabilities;


    public int getStatusFlags() {
        return statusFlags;
    }

    public void setStatusFlags(int statusFlags) {
        this.statusFlags = statusFlags;
    }

    public EOFPacket withStatusFlags(int statusFlags) {
        this.statusFlags = statusFlags;
        return this;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public int getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(int capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        resultBuffer.write((byte) 0xFE);
        resultBuffer.writeUB2(this.warnings);
        resultBuffer.writeUB2(this.statusFlags);
//        if (CapabilityFlag.isFlagSet(capabilities, CapabilityFlag.CLIENT_PROTOCOL_41)) {
//        }
    }
}
