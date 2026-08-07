package org.kendar.mssql.fsm;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.mssql.messages.PreLoginResponse;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class PreLogin extends TdsState {
    public static final byte ENCRYPT_OFF = 0x00;
    public static final byte ENCRYPT_ON = 0x01;
    public static final byte ENCRYPT_NOT_SUP = 0x02;
    public static final byte ENCRYPT_REQ = 0x03;
    private static final Logger log = LoggerFactory.getLogger(PreLogin.class);

    public PreLogin(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getPacketType() {
        return TdsPacketType.PRELOGIN;
    }

    @Override
    public boolean canRun(TdsPacket event) {
        if (!super.canRun(event)) return false;
        //A 0x12 packet carrying a TLS record belongs to the ssl handshake
        return event.getBuffer().size() == 0 || event.getBuffer().get(0) != 0x16;
    }

    @Override
    protected Iterator<ProtoStep> executeTds(MssqlBBuffer inputBuffer, MssqlProtoContext protoContext, TdsPacket event) {
        var clientEncryption = ENCRYPT_OFF;
        //Walk the option table (token, offset BE, length BE)
        while (inputBuffer.getPosition() < inputBuffer.size()) {
            var token = inputBuffer.get() & 0xFF;
            if (token == 0xFF) break;
            var offset = ((inputBuffer.get() & 0xFF) << 8) | (inputBuffer.get() & 0xFF);
            var length = ((inputBuffer.get() & 0xFF) << 8) | (inputBuffer.get() & 0xFF);
            if (token == 0x01 && length > 0 && (offset + length) <= inputBuffer.size()) {
                clientEncryption = inputBuffer.get(offset);
            }
        }
        var useTls = protoContext.getDescriptor().getSettings().isUseTls();
        byte responseEncryption;
        if (useTls && clientEncryption != ENCRYPT_NOT_SUP) {
            responseEncryption = ENCRYPT_ON;
            protoContext.setValue("SSL", true);
        } else {
            responseEncryption = ENCRYPT_NOT_SUP;
        }
        log.debug("[SERVER][PRELOGIN] client encryption {} response {}", clientEncryption, responseEncryption);
        return iteratorOfList(new PreLoginResponse(responseEncryption, protoContext.getSpid()));
    }
}
