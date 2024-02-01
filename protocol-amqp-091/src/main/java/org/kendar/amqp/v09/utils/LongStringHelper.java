package org.kendar.amqp.v09.utils;

import org.kendar.buffers.BBuffer;

import java.nio.charset.StandardCharsets;

public class LongStringHelper {
    private String str;

    public LongStringHelper(String str){

        this.str = str;
    }

    public static String read(BBuffer io) {
        var length = io.getInt();
        var bytes = io.getBytes((int)length);
        return new String(bytes);
    }
    public void write(BBuffer io) {

        io.writeInt(str.length());
        io.write(str.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return str;
    }
}
