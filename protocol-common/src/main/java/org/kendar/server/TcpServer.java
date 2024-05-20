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

/**
 * Multithreaded asynchronous server
 */
public class TcpServer {

    /**
     * Default host
     */
    private static final String HOST = "*";
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);
    private final NetworkProtoDescriptor protoDescriptor;

    /**
     * Listener thread
     */
    private Thread thread;

    /**
     * Listener socket
     */
    private AsynchronousServerSocketChannel server;

    public TcpServer(NetworkProtoDescriptor protoDescriptor) {
        this.protoDescriptor = protoDescriptor;
    }

    /**
     * Stop the server
     */
    public void stop() {
        try {
            server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            var proxy = protoDescriptor.getProxy();
            if (proxy != null && !proxy.isReplayer()) {
                var storage = protoDescriptor.getProxy().getStorage();
                if (storage != null) {
                    storage.optimize();
                }
            }
        }
    }

    /**
     * Start the server
     */
    public void start() {
        this.thread = new Thread(() -> {
            try {
                run();
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        this.thread.start();
    }

    /**
     * Really listen
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void run() throws IOException, ExecutionException, InterruptedException {
        //Executor for the asynchronous requests
        ExecutorService executor = Executors.newCachedThreadPool();
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);

        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group)) {
            this.server = server;
            //Setup buffer and listening
            server.setOption(StandardSocketOptions.SO_RCVBUF, 4096);
            server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            server.bind(new InetSocketAddress(protoDescriptor.getPort()));
            log.info("[SERVER] Listening on " + HOST + ":" + protoDescriptor.getPort());

            //noinspection InfiniteLoopStatement
            while (true) {
                //Accept request
                Future<AsynchronousSocketChannel> future = server.accept();
                try {
                    //Initialize client wrapper
                    var client = new TcpServerChannel(future.get());
                    log.info("[SERVER] Accepted connection from " + client.getRemoteAddress());
                    //Prepare the native buffer
                    ByteBuffer buffer = ByteBuffer.allocate(4096);
                    //Create the execution context
                    var context = (NetworkProtoContext) protoDescriptor.buildContext(client);
                    //Send the greetings
                    if (protoDescriptor.sendImmediateGreeting()) {
                        context.sendGreetings();
                    }
                    //Start reading
                    client.read(buffer, 30000, TimeUnit.MILLISECONDS, buffer, new CompletionHandler<>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            try {
                                attachment.flip();
                                if (result != -1 || attachment.remaining() > 0) {
                                    //If there is something
                                    var byteArray = new byte[attachment.remaining()];
                                    attachment.get(byteArray);
                                    log.trace("[SERVER][RX]: " + byteArray.length);
                                    var bb = context.buildBuffer();
                                    bb.write(byteArray);
                                    //Generate a BytesEvent and send it
                                    context.send(new BytesEvent(context, null, bb));

                                }
                                attachment.clear();
                                if (!client.isOpen()) {
                                    return;
                                }
                                //Restart reading again
                                client.read(attachment, 30000, TimeUnit.MILLISECONDS, attachment, this);
                            } catch (Exception ex) {
                                context.handleExceptionInternal(ex);
                                throw ex;
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            log.trace("Connection failed", exc);
                        }
                    });

                } catch (ExecutionException e) {
                    log.trace("Execution exception", e);
                }
            }
        }
    }
}