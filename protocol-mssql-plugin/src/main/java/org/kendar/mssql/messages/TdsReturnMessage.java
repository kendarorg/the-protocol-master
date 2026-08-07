package org.kendar.mssql.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.protocol.messages.NetworkReturnMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Token stream response: buffers all the tokens and splits them in
 * TABULAR_RESULT (0x04) packets of at most the negotiated packet size
 */
public class TdsReturnMessage implements NetworkReturnMessage {
    private final List<TdsToken> tokens = new ArrayList<>();
    private final int packetSize;
    private final int spid;

    public TdsReturnMessage(int packetSize, int spid) {
        this.packetSize = packetSize;
        this.spid = spid;
    }

    public TdsReturnMessage add(TdsToken token) {
        tokens.add(token);
        return this;
    }

    public TdsReturnMessage addAll(List<TdsToken> toAdd) {
        tokens.addAll(toAdd);
        return this;
    }

    public List<TdsToken> getTokens() {
        return tokens;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        var payload = new MssqlBBuffer();
        for (var token : tokens) {
            token.write(payload);
        }
        var data = payload.toArray();
        var maxPayload = packetSize - 8;
        var offset = 0;
        var packetId = 1;
        do {
            var chunk = Math.min(maxPayload, data.length - offset);
            var last = (offset + chunk) >= data.length;
            resultBuffer.write(TdsPacketType.TABULAR_RESULT);
            resultBuffer.write((byte) (last ? 0x01 : 0x00));
            //Length and spid are big-endian
            resultBuffer.writeShort((short) (chunk + 8));
            resultBuffer.writeShort((short) spid);
            resultBuffer.write((byte) (packetId++ & 0xFF));
            resultBuffer.write((byte) 0x00);
            resultBuffer.write(Arrays.copyOfRange(data, offset, offset + chunk));
            offset += chunk;
        } while (offset < data.length);
    }
}
