package org.kendar.server.streams;


import org.kendar.server.events.WriteFinishedEvent;
import org.kendar.server.exchange.ExchangeImpl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChunkedOutputStream extends FilterOutputStream {
    /* max. amount of user data per chunk */
    final static int CHUNK_SIZE = 4096;
    /* allow 4 bytes for chunk-size plus 4 for CRLFs */
    final static int OFFSET = 6; /* initial <=4 bytes for len + CRLF */
    final ExchangeImpl t;
    private final byte[] buf = new byte[CHUNK_SIZE + OFFSET + 2];
    private boolean closed = false;
    private int pos = OFFSET;
    private int count = 0;

    public ChunkedOutputStream(ExchangeImpl t, OutputStream src) {
        super(src);
        this.t = t;
    }

    public void write(int b) throws IOException {
        if (closed) {
            throw new StreamClosedException();
        }
        buf[pos++] = (byte) b;
        count++;
        if (count == CHUNK_SIZE) {
            writeChunk();
        }
        assert count < CHUNK_SIZE;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new StreamClosedException();
        }
        int remain = CHUNK_SIZE - count;
        if (len > remain) {
            System.arraycopy(b, off, buf, pos, remain);
            count = CHUNK_SIZE;
            writeChunk();
            len -= remain;
            off += remain;
            while (len >= CHUNK_SIZE) {
                System.arraycopy(b, off, buf, OFFSET, CHUNK_SIZE);
                len -= CHUNK_SIZE;
                off += CHUNK_SIZE;
                count = CHUNK_SIZE;
                writeChunk();
            }
        }
        if (len > 0) {
            System.arraycopy(b, off, buf, pos, len);
            count += len;
            pos += len;
        }
        if (count == CHUNK_SIZE) {
            writeChunk();
        }
    }

    /**
     * write out a chunk , and reset the pointers
     * chunk does not have to be CHUNK_SIZE bytes
     * count must == number of user bytes (<= CHUNK_SIZE)
     */
    private void writeChunk() throws IOException {
        char[] c = Integer.toHexString(count).toCharArray();
        int clen = c.length;
        int startByte = 4 - clen;
        int i;
        for (i = 0; i < clen; i++) {
            buf[startByte + i] = (byte) c[i];
        }
        buf[startByte + (i++)] = '\r';
        buf[startByte + (i++)] = '\n';
        buf[startByte + (i++) + count] = '\r';
        buf[startByte + (i++) + count] = '\n';
        out.write(buf, startByte, i + count);
        count = 0;
        pos = OFFSET;
    }

    public void close() throws IOException {
        if (closed) {
            return;
        }
        flush();
        try {
            /* write an empty chunk */
            writeChunk();
            out.flush();
            LeftOverInputStream is = t.getOriginalInputStream();
            if (!is.isClosed()) {
                is.close();
            }
            /* some clients close the connection before empty chunk is sent */
        } catch (IOException ignored) {

        } finally {
            closed = true;
        }

        WriteFinishedEvent e = new WriteFinishedEvent(t);
        t.getHttpContext().getServerImpl().addEvent(e);
    }

    public void flush() throws IOException {
        if (closed) {
            throw new StreamClosedException();
        }
        if (count > 0) {
            writeChunk();
        }
        out.flush();
    }
}
