package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.dtos.Field;
import org.kendar.protocol.ReturnMessage;

import java.util.Base64;
import java.util.List;

public class DataRow extends ReturnMessage {
    private final List<String> values;
    private final List<Field> fields;

    public DataRow(List<String> byteRow, List<Field> fields) {

        this.values = byteRow;
        this.fields = fields;
    }

    @Override
    public void write(BBuffer buffer) {
        buffer.write((byte) 'D'); // +1 for msg type
        var position = buffer.getPosition();
        buffer.writeInt(0); // +4 for length
        var length = 4;
        buffer.writeShort((short) values.size()); // +2 for number of columns
        length += 2;
        for (int i = 0; i < values.size(); i++) {
            var value = values.get(i);
            var field = fields.get(i);
            length += 4;
            if (value == null) {
                buffer.writeInt(0);
            } else if (field.isByteContent()) {
                var bs = Base64.getDecoder().decode(value);
                buffer.writeInt(bs.length);
                buffer.write(bs);
                length += bs.length;
            } else {
                var stringBytes = value.getBytes();
                buffer.writeInt(stringBytes.length);
                buffer.write(stringBytes);
                length += stringBytes.length;
            }
        }
        buffer.writeInt(length, position);
    }
}
