package org.kendar.mysql.fsm;

import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.Language;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.messages.Handshake;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

import static org.kendar.mysql.constants.StatusFlag.SERVER_STATUS_AUTOCOMMIT;

public class ConnectionEstablished extends ProtoState {
    public ConnectionEstablished(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        return inputBuffer.size() == 0;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var protocContext = (MySQLProtoContext) event.getContext();
        var gp = new Handshake();
        gp.setProtocolNumber((byte) 10);
        gp.setPacketNumber(protocContext.getPacketNumber());
        gp.setServerVersion("8.1.0");
        gp.setThreadId(protocContext.getNewPid());
        gp.setServerCapabilities((short) CapabilityFlag.unsetFlag(0xFFFF, CapabilityFlag.CLIENT_SSL.getCode()));
        gp.setServerLanguage(Language.UTF8_GENERAL_CI);
        gp.setServerStatus((short) SERVER_STATUS_AUTOCOMMIT.getCode());
        gp.setExtendedServerCapabilities((short) 0xC27F);
        gp.setAuthenticationPlugin("mysql_native_password");
        return iteratorOfList(gp);
    }

}
