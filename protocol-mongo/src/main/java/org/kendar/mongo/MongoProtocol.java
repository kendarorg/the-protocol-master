package org.kendar.mongo;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.fsm.OpCompressed;
import org.kendar.mongo.fsm.OpMsg;
import org.kendar.mongo.fsm.OpQuery;
import org.kendar.mongo.fsm.events.CompressedDataEvent;
import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.mongo.fsm.events.OpQueryRequest;
import org.kendar.mongo.fsm.msg.CmdGeneric;
import org.kendar.mongo.fsm.msg.CmdHello;
import org.kendar.mongo.fsm.msg.CmdSaslContinue;
import org.kendar.mongo.fsm.msg.CmdSaslStart;
import org.kendar.mongo.fsm.query.QueryHello;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.settings.GlobalSettings;

import java.util.List;

@TpmService(tags = "mongodb")
public class MongoProtocol extends NetworkProtoDescriptor {
    private static final int PORT = 27017;
    private static final boolean IS_BIG_ENDIAN = false;
    private final int port;

    @TpmConstructor
    public MongoProtocol(GlobalSettings ini, MongoProtocolSettings settings, MongoProxy proxy,
                         @TpmNamed(tags = "mongodb") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());
    }

    @Override
    public boolean isLateConnect(){
        return true;
    }

    public MongoProtocol(int port) {
        this.port = port;
        var pp = new MongoProtocolSettings();
        pp.setPort(port);
        setSettings(pp);
    }

    public MongoProtocol() {
        this(PORT);
    }

    @Override
    protected void initializeProtocol() {


        initialize(new ProtoStateSwitchCase(
                new ProtoStateWhile(
                        new OpCompressed(BytesEvent.class).asOptional(),
                        new ProtoStateSwitchCase(
                                new ProtoStateSequence(
                                        new OpMsg(BytesEvent.class, CompressedDataEvent.class),
                                        new ProtoStateSwitchCase(
                                                new CmdGeneric(OpMsgRequest.class),
                                                new CmdHello(OpMsgRequest.class),
                                                new CmdSaslStart(OpMsgRequest.class),
                                                new ProtoStateWhile(
                                                        new CmdSaslContinue(OpMsgRequest.class).asOptional()
                                                )
                                        )
                                ),
                                new ProtoStateSequence(
                                        new OpQuery(BytesEvent.class, CompressedDataEvent.class),
                                        new QueryHello(OpQueryRequest.class)
                                )
                        )
                )
        ));

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
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor,
                                         int contextId) {
        return new MongoProtoContext(this, contextId);
    }
}
