package org.kendar.protocol.states;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.logging.ByteBufFormat;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.tcpserver.NettyServerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class SSLHandshake extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(SSLHandshake.class);

    public SSLHandshake(Class<?>... messages) {
        super(messages);
    }

    public static void decodeTls(byte[] data) {

        if (data == null || data.length < 5) {
            System.out.println("Not enough data for TLS record header");
            return;
        }

        int contentType = data[0] & 0xFF;
        int versionMajor = data[1] & 0xFF;
        int versionMinor = data[2] & 0xFF;
        int recordLength = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);

        System.out.println("=== TLS Record ===");
        System.out.println("Content Type: " + contentType +
                (contentType == 22 ? " (Handshake)" :
                        contentType == 20 ? " (ChangeCipherSpec)" :
                                contentType == 21 ? " (Alert)" :
                                        contentType == 23 ? " (ApplicationData)" : " (Unknown)"));

        System.out.println("Version: TLS " + versionMajor + "." + versionMinor);
        System.out.println("Record Length: " + recordLength);

        // If it's a Handshake record, decode handshake header
        if (contentType == 22) {

            if (data.length < 9) {
                System.out.println("Not enough data for Handshake header");
                return;
            }

            int handshakeType = data[5] & 0xFF;
            int handshakeLength =
                    ((data[6] & 0xFF) << 16) |
                            ((data[7] & 0xFF) << 8) |
                            (data[8] & 0xFF);

            System.out.println("--- Handshake ---");
            System.out.println("Handshake Type: " + handshakeType +
                    (handshakeType == 1 ? " (ClientHello)" :
                            handshakeType == 2 ? " (ServerHello)" :
                                    handshakeType == 11 ? " (Certificate)" :
                                            handshakeType == 12 ? " (ServerKeyExchange)" :
                                                    handshakeType == 14 ? " (ServerHelloDone)" :
                                                            handshakeType == 16 ? " (ClientKeyExchange)" :
                                                                    handshakeType == 20 ? " (Finished)" : " (Other)"));

            System.out.println("Handshake Length: " + handshakeLength);
        }
    }

    public static void initializeSslUpdate(BytesEvent event) {
        event.getContext().setValue("SSL", true);

        var context = (NetworkProtoContext) event.getContext();
        var client = (NettyServerChannel) context.getClient();
        var sslContext = context.getSslContext();
        var ctx = client.getChannelHandlerContext();
        var pipeline = ctx.pipeline();
        event.getContext().getValue("START_TLS", true);


        var sslHandler = sslContext.newHandler(ctx.alloc());


        pipeline.addFirst(sslHandler);

        //pipeline.addFirst("logger", new LoggingHandler(LogLevel.DEBUG, ByteBufFormat.HEX_DUMP));
        //pipeline.removeLast();
        //pipeline.addLast(new ChannelInboundHandlerAdapter() {});
        sslHandler.handshakeFuture().addListener(future -> {
            event.getContext().setValue("START_SSL_NEGOTIATION", false);
            event.getContext().setValue("START_TLS", false);
            if (future.isSuccess()) {
                log.debug("TLS handshake completed");
            } else {
                log.debug("TLS handshake failed", future.cause());
                ctx.close();
            }
        });
        log.error("TLS handshake started");
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        if (inputBuffer.size() < 3) return false;
        inputBuffer.setPosition(0);
        var isTls = inputBuffer.get() == 0x16;

        inputBuffer.setPosition(0);
        isTls &= event.getContext().getValue("SSL", false);
        if (isTls) {
            var data = inputBuffer.getBytes(5);
            int contentType = data[0] & 0xFF;
            int versionMajor = data[1] & 0xFF;
            int versionMinor = data[2] & 0xFF;
            int recordLength = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);

            inputBuffer.setPosition(0);
            if (inputBuffer.size() < recordLength) {
                throw new AskMoreDataException();
            }
            log.debug("Starting tls handshake, contentType={}, version={}, recordLength={}", contentType, versionMajor + "." + versionMinor, recordLength);
            return true;
        }
        return false;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        if(event.getContext().getValue("INITALIZE_SSL_HANDSHAKE", false)){
            initializeSslUpdate(event);
        }
        event.getContext().setValue("BYPASS_CONVERTERS", false);
        event.getContext().setValue("INITALIZE_SSL_HANDSHAKE", false);
        log.debug("TLS handshake");
        var context = (NetworkProtoContext) event.getContext();
        var client = (NettyServerChannel) context.getClient();
        var ctx = client.getChannelHandlerContext();
        var pipeline = ctx.pipeline();
        event.getBuffer().setPosition(0);
        var data =event.getBuffer().toArray();
        var tlsBuf = Unpooled.wrappedBuffer(data);
        pipeline.fireChannelRead(tlsBuf);
        var res = event.getBuffer().size();
        event.getBuffer().truncate(res);

        return iteratorOfList();
    }
}
