package org.kendar.sql.parser;

public class SqlParseResult {
    private final String value;
    private final SqlStringType type;

    public SqlParseResult(String value, SqlStringType type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return "SqlParseResult{" +
                "value='" + value + '\'' +
                ", type=" + type +
                '}';
    }

    public String getValue() {
        return value;
    }

    public SqlStringType getType() {
        return type;
    }
}
