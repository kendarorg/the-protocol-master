package org.kendar.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Interface for the communication between server and client
 */
public interface ClientServerChannel {
    /**
     * Close the connection
     *
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Write asynchronously on the channel
     *
     * @param response
     * @return
     */
    Future<Integer> write(ByteBuffer response);

    /**
     * Read asynchronously from the channel
     *
     * @param buffer
     * @param i
     * @param timeUnit
     * @param buffer1
     * @param completionHandler
     */
    void read(ByteBuffer buffer, int i, TimeUnit timeUnit, ByteBuffer buffer1, CompletionHandler<Integer, ByteBuffer> completionHandler);

    /**
     * Check if it's open
     *
     * @return
     */
    boolean isOpen();
}
