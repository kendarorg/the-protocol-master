package org.kendar.tcpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.kendar.events.EventsQueue;
import org.kendar.exceptions.TPMException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.net.ssl.SSLEngine;
import java.io.File;

public class NettyServer implements Server {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    private static final String HOST = "*";
    private final NetworkProtoDescriptor protoDescriptor;
    protected Runnable onStart;
    protected Runnable onStop;
    private boolean tlsEnabled = false;
    private boolean tlsEnabledFromStart = false;
    private File certificateFile;
    private File privateKeyFile;
    private boolean useSelfSignedCertificate = true;
    private SslContext sslContext;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private boolean callDurationTimes;
    public NettyServer(NetworkProtoDescriptor protoDescriptor) {
        this.protoDescriptor = protoDescriptor;
    }



    /**
     * Stop the server
     */
    public void stop() {
        if (protoDescriptor.isWrapper()) {
            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", "0")) {
                protoDescriptor.terminate();
                Sleeper.sleepNoException(1000, EventsQueue::isEmpty, true);
            }


        } else {
            try {
                if (serverChannel != null) {
                    serverChannel.close().sync();
                }

                if (workerGroup != null) {
                    workerGroup.shutdownGracefully();
                }

                if (bossGroup != null) {
                    bossGroup.shutdownGracefully();
                }

                try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", "0")) {
                    protoDescriptor.terminate();
                    Sleeper.sleepNoException(1000, EventsQueue::isEmpty, true);
                }
                this.nioEventLoopGroup.shutdownGracefully();
                this.bossGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
                this.workerGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS):
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TPMException(e);
            } finally {
                try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", "0")) {
                    var proxy = protoDescriptor.getProxy();
                    if (proxy != null) {
                        proxy.terminateFilters();
                    }

                }
                Sleeper.sleepNoException(1000, EventsQueue::isEmpty, true);
            }
        }

        if (onStop != null) {
            onStop.run();
        }
    }

    public void setOnStart(Runnable onStart) {
        this.onStart = onStart;
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    public ProtoDescriptor getProtoDescriptor() {
        return protoDescriptor;
    }

    @Override
    public void enableTls(File certificateFile, File privateKeyFile) {
        this.tlsEnabled = true;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
    }

    @Override
    public void enableSelfSignedTls() {
        this.tlsEnabled = true;
        this.useSelfSignedCertificate = true;
    }

    public void useCallDurationTimes(boolean callDurationTimes) {
        this.callDurationTimes = callDurationTimes;
    }

    public boolean isRunning() {
        if (protoDescriptor.isWrapper()) {
            return protoDescriptor.isWrapperRunning();
        }
        return serverChannel != null && serverChannel.isActive();
    }

    private NioEventLoopGroup nioEventLoopGroup;
    /**
     * Start the server
     */
    public void start() {
        if (protoDescriptor.isWrapper()) {
            try {
                protoDescriptor.cleanCounters();
                protoDescriptor.start();
            } catch (Exception e) {
                throw new TPMException(e);
            }
            return;
        }

        this.nioEventLoopGroup = new NioEventLoopGroup();
        protoDescriptor.setGroup(nioEventLoopGroup);
        var settings = protoDescriptor.getSettings();
        if (ByteProtocolSettings.class.isAssignableFrom(settings.getClass())) {
            var byteSettings = (ByteProtocolSettings) settings;
            if (byteSettings.isStartWithTls()) {
                enableSelfSignedTls();
            }
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        protoDescriptor.cleanCounters();
        protoDescriptor.start();

        try {
            this.tlsEnabled =this.protoDescriptor.getSettings().isUseTls();
            this.tlsEnabledFromStart = this.protoDescriptor.getSettings().isUseTlsFromStart();
            if(tlsEnabled) {
                try {
                    if (useSelfSignedCertificate) {
                        var ssc = new SelfSignedCertificate();
                        sslContext = SslContextBuilder
                                .forServer(ssc.certificate(), ssc.privateKey())

//                            .protocols("TLSv1.3")
//                            .clientAuth(ClientAuth.NONE)
                                .protocols("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                                .build();
                    } else {
                        sslContext = SslContextBuilder
                                .forServer(certificateFile, privateKeyFile)

//                            .protocols("TLSv1.3")
//                            .clientAuth(ClientAuth.NONE)
                                .protocols("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                                .build();
                    }
                    protoDescriptor.setSslContext(sslContext);

                    if (tlsEnabled) {
                        log.info("TLS enabled for server on port {}", protoDescriptor.getPort());
                    }
                }catch (Exception e) {
                        throw new TPMException("Failed to initialize TLS", e);
                    }

            }

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_RCVBUF, 4096)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            if (tlsEnabled && sslContext != null && tlsEnabledFromStart) {
                                SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
                                sslEngine.setUseClientMode(false);

                                ch.pipeline().addFirst("ssl", new SslHandler(sslEngine));
                            }

                            ch.pipeline().addLast(new ServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(protoDescriptor.getPort()).sync();
            serverChannel = future.channel();

            log.info("[CL>TP][IN] Listening on {}:{} {}",
                    HOST,
                    protoDescriptor.getPort(),
                    protoDescriptor.getClass().getSimpleName());

            if (onStart != null) {
                onStart.run();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TPMException(e);
        }
    }


    public class ServerHandler extends ChannelInboundHandlerAdapter {

        private NetworkProtoContext context;
        private int contextId;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            contextId = protoDescriptor.getCounter("CONTEXT_ID");

            try (final MDC.MDCCloseable mdc =
                         MDC.putCloseable("connection", String.valueOf(contextId))) {

                log.trace("[CL>TP] Accepted connection from {}",
                        ctx.channel().remoteAddress());

                context = (NetworkProtoContext)
                        protoDescriptor.buildContext(new NettyServerChannel(ctx.channel(), ctx), contextId);

                if (protoDescriptor.sendImmediateGreeting()) {
                    context.sendGreetings();
                }
            }catch (Exception ex){
                log.error("Error creating context",ex);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf in = (ByteBuf) msg;

            try (final MDC.MDCCloseable mdc =
                         MDC.putCloseable("connection", String.valueOf(contextId))) {

                int readable = in.readableBytes();
                if (readable > 0) {

                    byte[] bytes = new byte[readable];
                    in.readBytes(bytes);

                    log.debug("[CL>TP][RX]: Received bytes: {}", bytes.length);

                    var bb = context.buildBuffer();
                    context.setUseCallDurationTimes(callDurationTimes);
                    bb.write(bytes);

                    context.send(new BytesEvent(context, null, bb));
                }

            } catch (Exception ex) {
                context.handleExceptionInternal(ex);
                throw ex;
            } finally {
                in.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.trace("Connection failed", cause);
            ctx.close();
        }
    }
}

