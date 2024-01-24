package org.kendar.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface Channel {
    void close() throws IOException;

    Future<Integer> write(ByteBuffer response);

    void read(ByteBuffer buffer, int i, TimeUnit timeUnit, ByteBuffer buffer1, CompletionHandler<Integer, ByteBuffer> completionHandler);

    boolean isOpen();
}
