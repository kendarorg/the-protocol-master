package org.kendar.mssql.fsm;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.DoneStatus;
import org.kendar.mssql.constants.EnvChangeType;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.mssql.messages.*;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Login7 extends TdsState {
    private static final Logger log = LoggerFactory.getLogger(Login7.class);

    public Login7(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getPacketType() {
        return TdsPacketType.LOGIN7;
    }

    private String readOffsetString(MssqlBBuffer inputBuffer, int offset, int chars) {
        if (chars == 0 || (offset + chars * 2) > inputBuffer.size()) return "";
        return new String(inputBuffer.getBytes(offset, chars * 2), StandardCharsets.UTF_16LE);
    }

    @Override
    protected Iterator<ProtoStep> executeTds(MssqlBBuffer inputBuffer, MssqlProtoContext protoContext, TdsPacket event) {
        inputBuffer.readUIntLE(); //length
        inputBuffer.readUIntLE(); //tds version
        var packetSize = inputBuffer.readUIntLE();
        inputBuffer.readUIntLE(); //client prog version
        inputBuffer.readUIntLE(); //client pid
        inputBuffer.readUIntLE(); //connection id
        inputBuffer.get(); //option flags 1
        inputBuffer.get(); //option flags 2
        inputBuffer.get(); //type flags
        inputBuffer.get(); //option flags 3
        inputBuffer.readUIntLE(); //client timezone
        inputBuffer.readUIntLE(); //client lcid

        var ibHostName = inputBuffer.readUShortLE();
        var cchHostName = inputBuffer.readUShortLE();
        var ibUserName = inputBuffer.readUShortLE();
        var cchUserName = inputBuffer.readUShortLE();
        inputBuffer.readUShortLE(); //ibPassword
        inputBuffer.readUShortLE(); //cchPassword
        var ibAppName = inputBuffer.readUShortLE();
        var cchAppName = inputBuffer.readUShortLE();
        inputBuffer.readUShortLE(); //ibServerName
        inputBuffer.readUShortLE(); //cchServerName
        inputBuffer.readUShortLE(); //ibUnused
        inputBuffer.readUShortLE(); //cbUnused
        inputBuffer.readUShortLE(); //ibCltIntName
        inputBuffer.readUShortLE(); //cchCltIntName
        inputBuffer.readUShortLE(); //ibLanguage
        inputBuffer.readUShortLE(); //cchLanguage
        var ibDatabase = inputBuffer.readUShortLE();
        var cchDatabase = inputBuffer.readUShortLE();

        var hostName = readOffsetString(inputBuffer, ibHostName, cchHostName);
        var userName = readOffsetString(inputBuffer, ibUserName, cchUserName);
        var appName = readOffsetString(inputBuffer, ibAppName, cchAppName);
        var database = readOffsetString(inputBuffer, ibDatabase, cchDatabase);
        if (database.isEmpty()) {
            database = "master";
        }
        log.debug("[SERVER][LOGIN7] host={} user={} app={} db={}", hostName, userName, appName, database);
        protoContext.setValue("LOGIN", userName);
        protoContext.setValue("DATABASE", database);

        if (packetSize >= 512 && packetSize <= 32767) {
            protoContext.setPacketSize((int) packetSize);
        }

        var message = protoContext.newMessage()
                .add(new EnvChangeToken(EnvChangeType.DATABASE, database, "master"))
                .add(new InfoToken(5701, "Changed database context to '" + database + "'."))
                .add(new EnvChangeCollationToken(ColMetadataToken.DEFAULT_COLLATION))
                .add(new EnvChangeToken(EnvChangeType.PACKET_SIZE,
                        Integer.toString(protoContext.getPacketSize()),
                        Integer.toString(MssqlProtoContext.DEFAULT_PACKET_SIZE)))
                .add(new LoginAckToken())
                .add(new DoneToken(DoneStatus.DONE_FINAL, 0));
        return iteratorOfList(message);
    }
}
