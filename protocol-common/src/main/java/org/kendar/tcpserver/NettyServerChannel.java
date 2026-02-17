package org.kendar.tcpserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class NettyServerChannel implements ClientServerChannel {

    private final Channel channel;
    private volatile boolean closed = false;

    public NettyServerChannel(Channel channel) {
        this.channel = Objects.requireNonNull(channel);
    }


    public SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        channel.close();
    }

    @Override
    public Future<Integer> write(ByteBuffer response) {

        if (closed || !channel.isActive()) {
            return CompletableFuture.completedFuture(0);
        }

        // Convert ByteBuffer -> ByteBuf
        ByteBuf byteBuf = Unpooled.wrappedBuffer(response);

        ChannelFuture nettyFuture = channel.writeAndFlush(byteBuf);

        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

        nettyFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                resultFuture.complete(response.remaining());
            } else {
                resultFuture.completeExceptionally(future.cause());
            }
        });

        return resultFuture;
    }

    @Override
    public void read(ByteBuffer buffer, int timeoutInMs, ByteBuffer buffer1, CompletionHandler<Integer, ByteBuffer> completionHandler) {


        /*
         * IMPORTANT:
         * Netty does not support "manual read with CompletionHandler".
         * Reads are event-driven via ChannelInboundHandler.channelRead().
         *
         * This method is intentionally unsupported because in Netty
         * the ServerHandler handles inbound data.
         */

        throw new UnsupportedOperationException(
                "Manual async read is not supported in Netty. " +
                        "Use ChannelInboundHandler.channelRead()."
        );
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen() && channel.isActive() && !closed;
    }

    public Channel getChannel() {
        return channel;
    }
}