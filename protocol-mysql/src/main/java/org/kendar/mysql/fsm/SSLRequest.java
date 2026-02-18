package org.kendar.mysql.fsm;

import org.kendar.mysql.MySqlProtocolSettings;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.SSLHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class SSLRequest extends MySQLProtoState {
    public SSLRequest(Class<BytesEvent> bytesEventClass) {
        super(bytesEventClass);
    }

    @Override
    protected Iterator<ProtoStep> executeBytes(MySQLBBuffer inputBuffer, BytesEvent event, int packetLength, int packetIndex) {
        var clientCapabilities = inputBuffer.readUB4();
        var maxPacketSize = inputBuffer.readUB4();
        var languageByte = inputBuffer.get();
        var reserved = inputBuffer.getBytes(19);
        var extendedClientCapabilities = inputBuffer.readUB4();
        var isSsl = CapabilityFlag.isFlagSet(clientCapabilities, CapabilityFlag.CLIENT_SSL.getCode());

        if (isSsl) {
            SSLHandshake.initializeSslUpdate(event);
        }

        return iteratorOfRunner();
    }

    @Override
    public boolean canRun(BytesEvent event) {
        var useTls = ((MySqlProtocolSettings) event.getContext().getDescriptor().getSettings()).isUseTls();
        if (!useTls) {
            return false;
        }
        if (super.canRun(event)) {
            if (event.getBuffer().size() == (3 + 1) + (4 + 4 + 1 + 19 + 4)) {
                return true;
            }
        }
        return false;
    }

    private static final Logger log = LoggerFactory.getLogger(SSLRequest.class);
}
