package org.kendar.tcpserver;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic communication class with client
 */
public class TcpServerChannel implements ClientServerChannel {
    private final AsynchronousSocketChannel client;
    private final Object lock = new Object();
    private boolean closed = false;

    public TcpServerChannel(AsynchronousSocketChannel client) {
        this.client = client;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return this.client.getRemoteAddress();
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            this.client.shutdownInput();
        } catch (Exception e) {
        }
        try {
            this.client.shutdownOutput();
        } catch (Exception e) {
        }
        try {
            this.client.close();
        } catch (Exception e) {
        }
    }

    @Override
    public Future<Integer> write(ByteBuffer response) {
        if (closed) return new Future<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return true;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Integer get() throws InterruptedException, ExecutionException {
                return 0;
            }

            @Override
            public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return 0;
            }
        };
        synchronized (lock) {
            return this.client.write(response);
        }
    }

    @Override
    public void read(ByteBuffer buffer, int timeoutInMs, ByteBuffer buffer1, CompletionHandler<Integer, ByteBuffer> completionHandler) {
        if (closed) return;
        this.client.read(buffer, timeoutInMs, TimeUnit.MILLISECONDS, buffer1, completionHandler);
    }

    @Override
    public boolean isOpen() {
        return client.isOpen() && !this.closed;
    }
}
