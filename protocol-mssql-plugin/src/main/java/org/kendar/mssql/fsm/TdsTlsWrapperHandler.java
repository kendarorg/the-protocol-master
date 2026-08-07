package org.kendar.mssql.fsm;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.kendar.mssql.constants.TdsPacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * During the TLS handshake the records travel wrapped in TDS PRELOGIN
 * (0x12) packets in both directions. This duplex handler sits in front
 * of the SslHandler: inbound it strips the TDS headers, outbound it
 * buffers the handshake records produced by the SslHandler and wraps
 * them, on flush, into a single TDS message (the clients feed exactly
 * one TDS message per handshake exchange to their SSL engine). It is
 * removed once the handshake completes (afterwards TLS flows raw)
 */
public class TdsTlsWrapperHandler extends ChannelDuplexHandler {
    public static final String NAME = "tpm-tds-tls-wrapper";
    private static final int HEADER_SIZE = 8;
    private static final int PACKET_SIZE = 4096;
    private static final Logger log = LoggerFactory.getLogger(TdsTlsWrapperHandler.class);
    private final List<ByteBuf> pendingOut = new ArrayList<>();
    private final List<ChannelPromise> pendingPromises = new ArrayList<>();
    private ByteBuf cumulation;
    private volatile boolean passthrough;

    /**
     * Once the handshake completes the wrapping phase is over and the
     * handler becomes transparent (TLS flows raw)
     */
    public void setPassthrough() {
        this.passthrough = true;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (passthrough || !(msg instanceof ByteBuf buf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (log.isDebugEnabled()) {
            var dump = new StringBuilder();
            for (var i = 0; i < Math.min(16, buf.readableBytes()); i++) {
                dump.append(String.format("%02X ", buf.getByte(buf.readerIndex() + i)));
            }
            log.debug("[SERVER][TLS] Wrapper inbound {} bytes: {}", buf.readableBytes(), dump);
        }
        if (cumulation == null) {
            cumulation = ctx.alloc().buffer();
        }
        cumulation.writeBytes(buf);
        buf.release();
        while (cumulation.isReadable()) {
            var first = cumulation.getUnsignedByte(cumulation.readerIndex());
            if (first == (TdsPacketType.PRELOGIN & 0xFF)) {
                if (cumulation.readableBytes() < HEADER_SIZE) {
                    break;
                }
                var length = cumulation.getUnsignedShort(cumulation.readerIndex() + 2);
                if (length < HEADER_SIZE || cumulation.readableBytes() < length) {
                    break;
                }
                cumulation.skipBytes(HEADER_SIZE);
                var payload = cumulation.readRetainedSlice(length - HEADER_SIZE);
                log.debug("[SERVER][TLS] Wrapper unwrapped {} bytes", payload.readableBytes());
                ctx.fireChannelRead(payload);
            } else {
                //Depending on the TLS version parts of the handshake can
                //travel as raw TLS records (the SslHandler does its own
                //record framing)
                var raw = cumulation.readRetainedSlice(cumulation.readableBytes());
                log.debug("[SERVER][TLS] Wrapper raw passthrough {} bytes", raw.readableBytes());
                ctx.fireChannelRead(raw);
            }
        }
        cumulation.discardReadBytes();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        log.debug("[SERVER][TLS] Wrapper read complete");
        ctx.fireChannelReadComplete();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (passthrough || !(msg instanceof ByteBuf buf)) {
            ctx.write(msg, promise);
            return;
        }
        pendingOut.add(buf);
        pendingPromises.add(promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        if (pendingOut.isEmpty()) {
            ctx.flush();
            return;
        }
        flushWrapped(ctx);
    }

    private void flushWrapped(ChannelHandlerContext ctx) {
        var payload = ctx.alloc().buffer();
        for (var buf : pendingOut) {
            payload.writeBytes(buf);
            buf.release();
        }
        pendingOut.clear();
        var promises = new ArrayList<>(pendingPromises);
        pendingPromises.clear();
        log.debug("[SERVER][TLS] Wrapper outbound message of {} bytes", payload.readableBytes());

        var maxPayload = PACKET_SIZE - HEADER_SIZE;
        while (payload.readableBytes() > maxPayload) {
            ctx.write(wrap(ctx, payload.readRetainedSlice(maxPayload), false));
        }
        var last = payload.readRetainedSlice(payload.readableBytes());
        payload.release();
        ctx.write(wrap(ctx, last, true)).addListener(future -> {
            for (var promise : promises) {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            }
        });
        ctx.flush();
    }

    private ByteBuf wrap(ChannelHandlerContext ctx, ByteBuf payload, boolean eom) {
        var result = ctx.alloc().buffer(payload.readableBytes() + HEADER_SIZE);
        result.writeByte(TdsPacketType.PRELOGIN);
        result.writeByte(eom ? 0x01 : 0x00);
        result.writeShort(payload.readableBytes() + HEADER_SIZE);
        result.writeShort(0);
        result.writeByte(0);
        result.writeByte(0);
        result.writeBytes(payload);
        payload.release();
        return result;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (cumulation != null) {
            if (cumulation.isReadable()) {
                //Bytes received after the handshake completed, they are
                //raw TLS records for the SslHandler
                ctx.fireChannelRead(cumulation);
            } else {
                cumulation.release();
            }
            cumulation = null;
        }
        for (var buf : pendingOut) {
            buf.release();
        }
        pendingOut.clear();
        for (var promise : pendingPromises) {
            promise.trySuccess();
        }
        pendingPromises.clear();
    }
}
