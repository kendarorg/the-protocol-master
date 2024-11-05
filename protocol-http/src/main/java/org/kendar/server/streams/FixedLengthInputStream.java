package org.kendar.server.streams;


import org.kendar.server.exchange.ExchangeImpl;

import java.io.IOException;
import java.io.InputStream;

public class FixedLengthInputStream extends LeftOverInputStream {
    private long remaining;

    public FixedLengthInputStream(ExchangeImpl t, InputStream src, long len) {
        super(t, src);
        if (len < 0L) {
            throw new IllegalArgumentException("Content-Length: " + len);
        } else {
            this.remaining = len;
        }
    }

    protected int readImpl(byte[] b, int off, int len) throws IOException {
        this.eof = this.remaining == 0L;
        if (this.eof) {
            return -1;
        } else {
            if ((long) len > this.remaining) {
                len = (int) this.remaining;
            }

            int n = this.in.read(b, off, len);
            if (n > -1) {
                this.remaining -= (long) n;
                if (this.remaining == 0L) {
                    this.t.getServerImpl().requestCompleted(this.t.getConnection());
                }
            }

            if (n < 0 && !this.eof) {
                throw new IOException("connection closed before all data received");
            } else {
                return n;
            }
        }
    }

    public int available() throws IOException {
        if (this.eof) {
            return 0;
        } else {
            int n = this.in.available();
            return (long) n < this.remaining ? n : (int) this.remaining;
        }
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int l) {
    }

    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
