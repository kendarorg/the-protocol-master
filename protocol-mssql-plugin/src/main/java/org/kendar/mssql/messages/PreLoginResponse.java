package org.kendar.mssql.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.protocol.messages.NetworkReturnMessage;

/**
 * PRELOGIN response, sent as a single TABULAR_RESULT packet with the
 * option table (offsets and lengths big-endian)
 */
public class PreLoginResponse implements NetworkReturnMessage {

    private final byte encryption;
    private final int spid;

    public PreLoginResponse(byte encryption, int spid) {
        this.encryption = encryption;
        this.spid = spid;
    }

    @Override
    public void write(BBuffer buffer) {
        //VERSION(6) ENCRYPTION(1) INSTOPT(1) THREADID(0) MARS(1)
        //5 option headers (5 bytes each) plus the 0xFF terminator
        var headersSize = 5 * 5 + 1;
        var version = new byte[]{0x10, 0x00, 0x08, 0x00, 0x00, 0x00};
        var payloadSize = headersSize + version.length + 1 + 1 + 1;

        buffer.write(TdsPacketType.TABULAR_RESULT);
        buffer.write((byte) 0x01);
        buffer.writeShort((short) (payloadSize + 8));
        buffer.writeShort((short) spid);
        buffer.write((byte) 0x01);
        buffer.write((byte) 0x00);

        var offset = headersSize;
        //VERSION
        buffer.write((byte) 0x00);
        buffer.writeShort((short) offset);
        buffer.writeShort((short) version.length);
        offset += version.length;
        //ENCRYPTION
        buffer.write((byte) 0x01);
        buffer.writeShort((short) offset);
        buffer.writeShort((short) 1);
        offset += 1;
        //INSTOPT
        buffer.write((byte) 0x02);
        buffer.writeShort((short) offset);
        buffer.writeShort((short) 1);
        offset += 1;
        //MARS
        buffer.write((byte) 0x04);
        buffer.writeShort((short) offset);
        buffer.writeShort((short) 1);
        offset += 1;
        //THREADID (empty)
        buffer.write((byte) 0x03);
        buffer.writeShort((short) offset);
        buffer.writeShort((short) 0);
        //TERMINATOR
        buffer.write((byte) 0xFF);

        buffer.write(version);
        buffer.write(encryption);
        buffer.write((byte) 0x00);
        buffer.write((byte) 0x00);
    }
}
