package org.kendar.amqp.v09.utils;

import org.kendar.buffers.BBuffer;

import java.nio.charset.StandardCharsets;

public class ShortStringHelper {
    private final String str;

    public ShortStringHelper(String str) {

        this.str = str;
    }

    public static String read(BBuffer io) {
        var length = (int) io.get();
        var bytes = io.getBytes(length);
        return new String(bytes);
    }

    public void write(BBuffer io) {
        if (str == null) {
            io.write((byte) 0);
        } else {
            io.write((byte) str.length());
            io.write(str.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public String toString() {
        return str;
    }
}
