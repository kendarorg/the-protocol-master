package org.kendar.mssql.fsm;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

public class SqlBatch extends TdsState {
    private static final Logger log = LoggerFactory.getLogger(SqlBatch.class);

    public SqlBatch(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getPacketType() {
        return TdsPacketType.SQL_BATCH;
    }

    @Override
    protected Iterator<ProtoStep> executeTds(MssqlBBuffer inputBuffer, MssqlProtoContext protoContext, TdsPacket event) {
        skipAllHeaders(inputBuffer);
        var remaining = inputBuffer.size() - inputBuffer.getPosition();
        var query = new String(inputBuffer.getBytes(remaining), StandardCharsets.UTF_16LE);
        log.info("[SERVER][QUERY][1]: {}", query);
        var tokens = protoContext.getExecutor().executeQuery(protoContext, query, new ArrayList<>(), false);
        return iteratorOfList(protoContext.newMessage().addAll(tokens));
    }
}
