package org.kendar.tcpserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

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
     * @param timeoutInMs
     * @param buffer1
     * @param completionHandler
     */
    void read(ByteBuffer buffer, int timeoutInMs, ByteBuffer buffer1, CompletionHandler<Integer, ByteBuffer> completionHandler);

    /**
     * Check if it's open
     *
     * @return
     */
    boolean isOpen();
}
