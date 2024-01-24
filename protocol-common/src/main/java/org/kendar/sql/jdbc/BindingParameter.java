package org.kendar.sql.jdbc;

import java.sql.JDBCType;

public class BindingParameter {
    private boolean output;
    private JDBCType type;



    public void setOutput(boolean output) {
        this.output = output;
    }

    public void setType(JDBCType type) {
        this.type = type;
    }

    public boolean binary;
    private String value;

    public BindingParameter(){

    }
    public BindingParameter(String value, boolean binary, boolean output, int type) {
        this.binary = binary;
        this.value = value;
        this.output = output;
        this.type = JDBCType.valueOf(type);
    }

    public BindingParameter(String value, boolean binary, boolean output, JDBCType type) {
        this.binary = binary;
        this.value = value;
        this.output = output;
        this.type = type;
    }

    public BindingParameter(String value, boolean binary, int type) {
        this(value, binary, false, type);
    }

    public BindingParameter(String value, boolean binary, JDBCType type) {
        this(value, binary, false, type);
    }

    public boolean isOutput() {
        return output;
    }

    public boolean isBinary() {
        return binary;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JDBCType getType() {
        return type;
    }
}
