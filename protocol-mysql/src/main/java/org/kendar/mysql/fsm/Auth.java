package org.kendar.mysql.fsm;

import org.kendar.events.EventsQueue;
import org.kendar.events.JdbcConnect;
import org.kendar.mysql.MySqlProtocolSettings;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.CapabilityFlag;
import org.kendar.mysql.constants.Language;
import org.kendar.mysql.constants.StatusFlag;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.messages.OkPacket;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
        byte[] password = new byte[]{};
        String database = "";
        String clientPluginName="";
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
        password = untilZero(password);
        var fullThing = clientPluginName+"|"+userName + "|" + new String(password) + "|" + database;

        protocContext.setValue("userid", userName);
        protocContext.setValue("database", database);
        protocContext.setValue("password", new String(password));
        inputBuffer.setPosition(packetLength + 4);
        var toSend = new OkPacket();
        var force3Bytes = ((MySqlProtocolSettings) event.getContext().getDescriptor().getSettings()).isForce3BytesOkPacketInfo();
        toSend.setForce3BytesOkPacketInfo(force3Bytes);
        toSend.setPacketNumber(packetIndex + 1);
        toSend.setCapabilities(CapabilityFlag.getFakeServerCapabilities());
        toSend.setStatusFlags(StatusFlag.SERVER_STATUS_AUTOCOMMIT.getCode());
        EventsQueue.getInstance().handle(new JdbcConnect(protocContext));
        return iteratorOfList(toSend);
    }

    public static byte[] untilZero(byte[] input) {
        int i = 0;
        while (i < input.length && input[i] != 0) {
            i++;
        }
        return Arrays.copyOfRange(input, 0, i);
    }

    private static final Logger log = LoggerFactory.getLogger(Auth.class);
}
