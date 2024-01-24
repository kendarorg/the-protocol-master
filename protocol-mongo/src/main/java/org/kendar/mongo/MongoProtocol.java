package org.kendar.mongo;

import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.fsm.OpCompressed;
import org.kendar.mongo.fsm.OpMsg;
import org.kendar.mongo.fsm.OpQuery;
import org.kendar.mongo.fsm.events.CompressedDataEvent;
import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.mongo.fsm.events.OpQueryRequest;
import org.kendar.mongo.fsm.msg.CmdGeneric;
import org.kendar.mongo.fsm.msg.CmdHello;
import org.kendar.mongo.fsm.msg.CmdSaslStart;
import org.kendar.mongo.fsm.query.QueryHello;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.protocol.fsm.Start;
import org.kendar.server.Channel;

public class MongoProtocol extends ProtoDescriptor {
    private static final int PORT = 27017;
    private static final boolean IS_BIG_ENDIAN = false;
    private final int port;

    public MongoProtocol(int port) {
        this.port = port;
    }

    public MongoProtocol() {
        this(PORT);
    }

    @Override
    protected void initializeProtocol() {
        addState(new Start(),
                new OpMsg(BytesEvent.class),
                new OpQuery(BytesEvent.class),
                new OpCompressed(BytesEvent.class));

        addState(new OpCompressed(),
                new OpQuery(BytesEvent.class, CompressedDataEvent.class),
                new OpMsg(BytesEvent.class, CompressedDataEvent.class));

        addState(new OpMsg(),
                new CmdHello(OpMsgRequest.class),
                new CmdGeneric(OpMsgRequest.class),
                new CmdSaslStart(OpMsgRequest.class));

        addState(new OpQuery(),
                new QueryHello(OpQueryRequest.class));

        addState(new QueryHello(),
                new OpMsg(BytesEvent.class),
                new OpCompressed(BytesEvent.class));

        addState(new CmdGeneric(),
                new OpMsg(BytesEvent.class));

        addState(new CmdSaslStart(),
                new OpMsg(BytesEvent.class));

        addState(new CmdHello(),
                new OpMsg(BytesEvent.class));

    }

    @Override
    public boolean isBe() {
        return IS_BIG_ENDIAN;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, Channel client) {
        var result = new MongoProtoContext(this, client);
        return result;
    }
}
