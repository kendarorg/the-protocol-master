package org.kendar.sql.jdbc;

public class SelectColumn {
    private boolean bytes;
    private String value;

    public boolean isBytes() {
        return bytes;
    }

    public void setBytes(boolean bytes) {
        this.bytes = bytes;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
