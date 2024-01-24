package org.kendar.postgres.dtos;

public class Field {
    private String name;
    private boolean byteContent;

    public Field() {

    }

    public Field(String name, boolean byteContent) {
        this.name = name;
        this.byteContent = byteContent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isByteContent() {
        return byteContent;
    }

    public void setByteContent(boolean byteContent) {
        this.byteContent = byteContent;
    }

}
