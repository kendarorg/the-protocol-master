package org.kendar.mysql.fsm;

import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.CommandType;
import org.kendar.mysql.constants.StatusFlag;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.mysql.messages.OkPacket;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public class ComRefresh extends ProtoState {
    public ComRefresh(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(CommandEvent event) {
        return event.getCommandType() == CommandType.COM_REFRESH;
    }

    public Iterator<ProtoStep> execute(CommandEvent event) {
        var toSend = new OkPacket();
        toSend.setPacketNumber(1);
        toSend.setCapabilities(CapabilityFlag.getFakeServerCapabilities());
        toSend.setStatusFlags(StatusFlag.SERVER_STATUS_AUTOCOMMIT.getCode());
        return iteratorOfList(toSend);

    }
}
