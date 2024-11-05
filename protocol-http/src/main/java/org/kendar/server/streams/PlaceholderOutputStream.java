package org.kendar.server.streams;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream which wraps another stream
 * which is supplied either at creation time, or sometime later.
 * If a caller/user tries to write to this stream before
 * the wrapped stream has been provided, then an IOException will
 * be thrown.
 */
public class PlaceholderOutputStream extends java.io.OutputStream {

    OutputStream wrapped;

    public PlaceholderOutputStream(OutputStream os) {
        wrapped = os;
    }

    public void setWrappedStream(OutputStream os) {
        wrapped = os;
    }

    public boolean isWrapped() {
        return wrapped != null;
    }

    private void checkWrap() throws IOException {
        if (wrapped == null) {
            throw new IOException("response headers not sent yet");
        }
    }

    public void write(int b) throws IOException {
        checkWrap();
        wrapped.write(b);
    }

    public void write(byte b[]) throws IOException {
        checkWrap();
        wrapped.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        checkWrap();
        wrapped.write(b, off, len);
    }

    public void flush() throws IOException {
        checkWrap();
        wrapped.flush();
    }

    public void close() throws IOException {
        checkWrap();
        wrapped.close();
    }
}
