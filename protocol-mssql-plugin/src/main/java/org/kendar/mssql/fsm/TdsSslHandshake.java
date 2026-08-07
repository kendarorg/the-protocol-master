package org.kendar.mssql.fsm;

import io.netty.buffer.Unpooled;
import org.kendar.mssql.constants.TdsPacketType;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.tcpserver.NettyServerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * TLS handshake for TDS: the handshake records arrive wrapped in
 * PRELOGIN packets (already reassembled by the TdsPacketTranslator).
 * On the first record the SslHandler is installed together with the
 * TdsTlsWrapperHandler which wraps/unwraps the following handshake
 * records; once the handshake completes the wrapper is removed and the
 * decrypted TDS stream flows through the normal state machine
 */
public class TdsSslHandshake extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(TdsSslHandshake.class);

    public TdsSslHandshake(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(TdsPacket event) {
        if (event.getPacketType() != TdsPacketType.PRELOGIN) {
            return false;
        }
        var buffer = event.getBuffer();
        if (buffer.size() == 0 || buffer.get(0) != 0x16) {
            return false;
        }
        return event.getContext().getValue("SSL", false);
    }

    public Iterator<ProtoStep> execute(TdsPacket event) {
        var context = (MssqlProtoContext) event.getContext();
        var client = (NettyServerChannel) context.getClient();
        var ctx = client.getChannelHandlerContext();
        var pipeline = ctx.pipeline();

        log.debug("[SERVER][TLS] Starting TDS tls handshake");
        var sslHandler = context.getSslContext().newHandler(ctx.alloc());
        //The PRELOGIN encapsulated negotiation is a TLS <= 1.2 flow (with
        //1.3 the client considers the handshake complete before flushing
        //its wrapped Finished message and the negotiation deadlocks)
        sslHandler.engine().setEnabledProtocols(new String[]{"TLSv1.2"});
        var wrapper = new TdsTlsWrapperHandler();
        pipeline.addFirst(sslHandler);
        pipeline.addFirst(TdsTlsWrapperHandler.NAME, wrapper);

        sslHandler.handshakeFuture().addListener(future -> {
            if (future.isSuccess()) {
                log.debug("[SERVER][TLS] TDS tls handshake completed");
                //Push out the final wrapped handshake records
                //(ChangeCipherSpec+Finished) then become transparent
                pipeline.flush();
                wrapper.setPassthrough();
            } else {
                log.debug("[SERVER][TLS] TDS tls handshake failed", future.cause());
                ctx.close();
            }
        });

        //Feed the first (already unwrapped) TLS record to the SslHandler
        var buffer = event.getBuffer();
        buffer.setPosition(0);
        var data = buffer.toArray();
        //Firing from the wrapper context delivers to the next inbound
        //handler, the SslHandler itself
        pipeline.context(TdsTlsWrapperHandler.NAME).fireChannelRead(Unpooled.wrappedBuffer(data));
        //The SslHandler flushes its handshake output on read complete
        pipeline.context(TdsTlsWrapperHandler.NAME).fireChannelReadComplete();
        buffer.truncate(buffer.size());
        return iteratorOfEmpty();
    }
}
