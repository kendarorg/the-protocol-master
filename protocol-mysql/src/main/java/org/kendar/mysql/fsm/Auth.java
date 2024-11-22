package org.kendar.mysql.fsm;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.Language;
import org.kendar.mysql.constants.StatusFlag;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.messages.OkPacket;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class Auth extends MySQLProtoState {
    public Auth(Class<?>... messages) {
        super(messages);
    }

    @Override
    public Iterator<ProtoStep> executeBytes(MySQLBBuffer inputBuffer, BytesEvent event, int packetLength, int packetIndex) {
        var clientFlag = inputBuffer.readUB4();

        var protocContext = (MySQLProtoContext) event.getContext();
        protocContext.setClientCapabilities(clientFlag);
        var maxPacketSize = inputBuffer.readUB4();
        var languageByte = inputBuffer.get();
        var language = Language.of(languageByte);
        inputBuffer.getBytes(23);
        var userName = inputBuffer.getString();
        byte[] password = null;
        String database;
        String clientPluginName;
        var authResponseLength = 0;
        if (CapabilityFlag.isFlagSet(clientFlag, CapabilityFlag.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA)) {
            password = inputBuffer.readBytesWithLength();
        } else if (CapabilityFlag.isFlagSet(clientFlag, CapabilityFlag.CLIENT_SECURE_CONNECTION)) {
            authResponseLength = inputBuffer.get();
            password = inputBuffer.getBytes(authResponseLength);
        }
        if (CapabilityFlag.isFlagSet(clientFlag, CapabilityFlag.CLIENT_CONNECT_WITH_DB)) {
            database = inputBuffer.getString();
        }
        if (CapabilityFlag.isFlagSet(clientFlag, CapabilityFlag.CLIENT_PLUGIN_AUTH)) {
            clientPluginName = inputBuffer.getString();
        }
        inputBuffer.setPosition(packetLength + 4);
        var toSend = new OkPacket();
        toSend.setPacketNumber(packetIndex + 1);
        toSend.setCapabilities(CapabilityFlag.getFakeServerCapabilities());
        toSend.setStatusFlags(StatusFlag.SERVER_STATUS_AUTOCOMMIT.getCode());
        return iteratorOfList(toSend);
    }
}
