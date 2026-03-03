package org.kendar.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.ProxyException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;

public abstract class NettyProxySocket extends BaseProxySocket {

    private static final Logger log = LoggerFactory.getLogger(NettyProxySocket.class);


    private final Bootstrap bootstrap;
    private final Channel channel;

    public NettyProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        this.context = context;
        try {
            var sslContext = context.getSslContext();
            var nioEventLoopGroup = ((NetworkProtoDescriptor) context.getDescriptor()).getGroup();
            BBuffer tempBuffer = context.buildBuffer();
            bootstrap = new Bootstrap();
            bootstrap.group(nioEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (sslContext != null) {
                                pipeline.addFirst(sslContext.newHandler(ch.alloc()));
                            }
                            pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    context.setActive();
                                    try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                                        byte[] bytes = new byte[msg.readableBytes()];
                                        msg.readBytes(bytes);
                                        onRead(tempBuffer, bytes);
                                    }
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    log.trace("[TP<SR][RX] Connected to server");
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                                        log.trace("[TP<SR][RX] Fail to read message from server", cause);
                                    }
                                    ctx.close();
                                }
                            });
                        }
                    });

            // Connect and wait for channel
            ChannelFuture future = bootstrap.connect(
                    inetSocketAddress.getAddress().getHostAddress()
                    , inetSocketAddress.getPort()).sync();
            channel = future.channel();

        } catch (Exception e) {
            throw new ProxyException(e);
        }
    }


    public void write(BBuffer buffer) {
        context.setActive();
        buffer.setPosition(0);
        Sleeper.sleepNoException(1000, channel::isWritable);
        try {
            channel.writeAndFlush(Unpooled.wrappedBuffer(buffer.getAll())).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            channel.close();
        } catch (Exception e) {
            log.trace("Error closing connection", e);
        }
    }

    public boolean isConnected() {
        return channel.isOpen();
    }
}
