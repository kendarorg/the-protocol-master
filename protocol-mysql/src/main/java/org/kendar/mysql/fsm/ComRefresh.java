package org.kendar.mysql.fsm;

import org.kendar.mysql.MySqlProtocolSettings;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.constants.StatusFlag;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.mysql.messages.OkPacket;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public class ComRefresh extends ProtoState {
    public ComRefresh(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {
        return event.getCommandType() == CommandType.COM_REFRESH;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var force3Bytes = ((MySqlProtocolSettings) event.getContext().getDescriptor().getSettings()).isForce3BytesOkPacketInfo();
        var toSend = new OkPacket();
        toSend.setForce3BytesOkPacketInfo(force3Bytes);
        toSend.setPacketNumber(1);
        toSend.setCapabilities(CapabilityFlag.getFakeServerCapabilities());
        toSend.setStatusFlags(StatusFlag.SERVER_STATUS_AUTOCOMMIT.getCode());
        return iteratorOfList(toSend);

    }
}
