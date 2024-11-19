package org.kendar.server.streams;


import org.kendar.http.utils.constants.ConstantsHeader;
import org.kendar.server.events.WriteFinishedEvent;
import org.kendar.server.exchange.ExchangeImpl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FixedLengthOutputStream extends FilterOutputStream {
    ExchangeImpl t;
    private long remaining;
    private boolean eof = false;
    private boolean closed = false;

    public FixedLengthOutputStream(ExchangeImpl t, OutputStream src, long len) {
        super(src);
        if (len < 0L) {
            throw new IllegalArgumentException(ConstantsHeader.CONTENT_LENGTH + ": " + len);
        } else {
            this.t = t;
            this.remaining = len;
        }
    }

    public void write(int b) throws IOException {
        if (this.closed) {
            throw new IOException("stream closed");
        } else {
            this.eof = this.remaining == 0L;
            if (this.eof) {
                throw new StreamClosedException();
            } else {
                this.out.write(b);
                --this.remaining;
            }
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (this.closed) {
            throw new IOException("stream closed");
        } else {
            this.eof = this.remaining == 0L;
            if (this.eof) {
                throw new StreamClosedException();
            } else if ((long) len > this.remaining) {
                throw new IOException("too many bytes to write to stream");
            } else {
                this.out.write(b, off, len);
                this.remaining -= len;
            }
        }
    }

    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            if (this.remaining > 0L) {
                this.t.close();
                throw new IOException("insufficient bytes written to stream");
            } else {
                this.flush();
                this.eof = true;
                LeftOverInputStream is = this.t.getOriginalInputStream();
                if (!is.isClosed()) {
                    try {
                        is.close();
                    } catch (IOException var3) {
                    }
                }

                WriteFinishedEvent e = new WriteFinishedEvent(this.t);
                this.t.getHttpContext().getServerImpl().addEvent(e);
            }
        }
    }
}
