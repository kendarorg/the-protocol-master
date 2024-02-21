package org.kendar.server;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.*;

public class TcpServer {
    private static final String HOST = "*";
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);
    private static boolean running = false;
    private final NetworkProtoDescriptor protoDescriptor;
    private Thread thread;
    private AsynchronousServerSocketChannel server;

    public TcpServer(NetworkProtoDescriptor protoDescriptor) {

        this.protoDescriptor = protoDescriptor;
    }

    public void stop() {
        try {
            running = false;
            server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        running = true;
        this.thread = new Thread(() -> {
            try {
                run();
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        this.thread.start();
    }

    @SuppressWarnings("CatchMayIgnoreException")
    private void run() throws IOException, ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);

        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group)) {
            this.server = server;
            server.setOption(StandardSocketOptions.SO_RCVBUF, 1024);
            server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            server.bind(new InetSocketAddress(protoDescriptor.getPort()));
            log.info("[SERVER] Listening on " + HOST + ":" + protoDescriptor.getPort());

            //noinspection InfiniteLoopStatement
            for (; ; ) {
                Future<AsynchronousSocketChannel> future = server.accept();
                try {
                    ClientServerChannel client = new TcpServerChannel(future.get());
                    log.info("[SERVER] Accepted connection from " + ((TcpServerChannel) client).getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(4096);
                    var context = (NetworkProtoContext) protoDescriptor.buildContext(client);

                    if (protoDescriptor.sendImmediateGreeting()) {
                        context.runGreetings();
                    }

                    var th = new Thread(context::start);
                    th.start();
                    client.read(buffer, 30000, TimeUnit.MILLISECONDS, buffer, new CompletionHandler<>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            try {
                                attachment.flip();
                                if (result != -1 || attachment.remaining() > 0) {
                                    var byteArray = new byte[attachment.remaining()];
                                    attachment.get(byteArray);
                                    log.trace("[SERVER][RX]: " + byteArray.length);
                                    var bb = context.buildBuffer();
                                    bb.write(byteArray);
                                    context.send(new BytesEvent(context, null, bb));

                                }
                                attachment.clear();
                                if (!client.isOpen()) {
                                    return;
                                }
                                client.read(attachment, 30000, TimeUnit.MILLISECONDS, attachment, this);
                            } catch (Exception ex) {
                                context.runException(ex);
                                throw ex;
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {

                        }
                    });
                } catch (ExecutionException e) {

                }
            }
        }
    }
}