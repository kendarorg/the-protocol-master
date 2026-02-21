package org.kendar.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.ProxyException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class NettyProxySocket extends BaseProxySocket{

    private static final Logger log = LoggerFactory.getLogger(NettyProxySocket.class);


    private static NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
    private final Bootstrap bootstrap;
    private final Channel channel;

    public NettyProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        this.context = context;
        try {
            var sslContext = context.getSslContext();

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
                            if(sslContext!=null) {
                                pipeline.addFirst(sslContext.newHandler(ch.alloc()));
                            }
                            pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    context.setActive();
                                    try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                                        byte[] bytes = new byte[msg.readableBytes()];
                                        msg.readBytes(bytes);
                                        onRead(tempBuffer,bytes);
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
            ChannelFuture future = bootstrap.connect(inetSocketAddress.getHostName(), inetSocketAddress.getPort()).sync();
            channel = future.channel();

        } catch (Exception e) {
            throw new ProxyException(e);
        }
    }


    public void write(BBuffer buffer) {
        context.setActive();
        buffer.setPosition(0);
        Sleeper.sleepNoException(1000, channel::isOpen);
        channel.writeAndFlush(Unpooled.wrappedBuffer(buffer.getAll()));
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
