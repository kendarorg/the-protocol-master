package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.constants.TypesOidsConstants;
import org.kendar.postgres.dtos.Field;
import org.kendar.protocol.messages.NetworkReturnMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class RowDescription implements NetworkReturnMessage {

    private final List<Field> fields;

    public RowDescription(List<Field> fields) {
        this.fields = fields;
    }


    @Override
    public void write(BBuffer buffer) {
        buffer.write((byte) 'T');
        var position = buffer.getPosition();
        buffer.writeInt(0);
        buffer.writeShort((short) fields.size());
        var length = 6;

        for (Field field : fields) {
            var bytes = field.getName().getBytes(StandardCharsets.UTF_8);
            buffer.write(bytes);
            length += bytes.length;
            buffer.write((byte) 0); // null-terminated
            length += 1;
            buffer.writeInt(0);
            length += 4;
            buffer.writeShort((short) 0);
            length += 2;
            buffer.writeInt(field.isByteContent() ? TypesOidsConstants.Varbit : TypesOidsConstants.Varchar);
            length += 4;
            buffer.writeShort((short) 2147483647);
            length += 2;
            buffer.writeInt(-1);
            length += 4;
            buffer.writeShort((short) (field.isByteContent() ? 1 : 0));
            length += 2;
        }
        buffer.writeInt(length, position);
    }
}
