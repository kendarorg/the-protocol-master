package org.kendar.mysql.fsm.events;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CommandType;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

public class CommandEvent extends ProtocolEvent {
    private final MySQLBBuffer buffer;
    private final CommandType commandType;

    public CommandEvent(ProtoContext context, Class<?> prevState, MySQLBBuffer buffer, CommandType commandType) {
        super(context, prevState);
        this.buffer = buffer;
        this.commandType = commandType;
    }

    public MySQLBBuffer getBuffer() {
        return buffer;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    @Override
    public String toString() {
        return "CommandEvent{" +
                "commandType=" + commandType +
                '}';
    }
}
