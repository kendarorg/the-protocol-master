package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;

import java.util.List;

public class DataRow extends MySQLReturnMessage {
    private final List<String> rows;

    public DataRow(List<String> rows) {

        this.rows = rows;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        for (var row : rows) {
            if (row == null) {
                resultBuffer.write((byte) 0xFB);
            } else {
                resultBuffer.writeWithLength(row.getBytes());
            }
        }
    }
}
