package org.kendar.tcpserver;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;
import java.util.concurrent.Future;

public class AsynchronousServerSSLSocketChannel implements AbstractAsynchronousServerSocketChannel{
    private final AsynchronousServerSocketChannel wrapped;
    private final SSLContext context;

    public AsynchronousServerSSLSocketChannel(AsynchronousServerSocketChannel wrapped, SSLContext context){
        this.wrapped = wrapped;
        this.context = context;
    }
    @Override
    public AsynchronousChannelProvider provider() {
        return wrapped.provider();
    }

    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress local) throws IOException {
        return wrapped.bind(local);
    }

    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        return wrapped.bind(local,backlog);
    }

    @Override
    public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        return wrapped.setOption(name,value);
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return wrapped.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return wrapped.supportedOptions();
    }

    @Override
    public <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        wrapped.accept(attachment,handler);
    }

    @Override
    public Future<AsynchronousSocketChannel> accept() {
        return wrapped.accept();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return wrapped.getLocalAddress();
    }

    @Override
    public boolean isOpen() {
        return wrapped.isOpen();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
