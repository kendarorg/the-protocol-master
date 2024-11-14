package org.kendar.http.utils;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NullEntity extends AbstractHttpEntity implements Cloneable {
    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return null;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {

    }

    @Override
    public boolean isStreaming() {
        return false;
    }
}
