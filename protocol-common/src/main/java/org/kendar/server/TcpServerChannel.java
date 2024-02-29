package org.kendar.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Basic communication class with client
 */
public class TcpServerChannel implements ClientServerChannel {
    private final AsynchronousSocketChannel client;

    public TcpServerChannel(AsynchronousSocketChannel client) {
        this.client = client;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return this.client.getRemoteAddress();
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }

    @Override
    public Future<Integer> write(ByteBuffer response) {
        return this.client.write(response);
    }

    @Override
    public void read(ByteBuffer buffer, int i, TimeUnit timeUnit, ByteBuffer buffer1, CompletionHandler<Integer, ByteBuffer> completionHandler) {
        this.client.read(buffer, i, timeUnit, buffer1, completionHandler);
    }

    @Override
    public boolean isOpen() {
        return client.isOpen();
    }
}
