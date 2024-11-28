package org.kendar.sql.jdbc;

import java.sql.JDBCType;

public class ProxyMetadata {
    private String columnName;

    private boolean byteData;
    private JDBCType columnType;
    private int precision;

    public ProxyMetadata() {

    }

    public ProxyMetadata(String columnName, boolean byteData) {
        this.columnName = columnName;
        this.byteData = byteData;
    }

    public ProxyMetadata(String columnName, boolean byteData, int columnType, int precision) {

        this.columnName = columnName;

        this.byteData = byteData;
        this.columnType = JDBCType.valueOf(columnType);
        this.precision = precision;
    }


    public JDBCType getColumnType() {
        return columnType;
    }

    public void setColumnType(JDBCType columnType) {
        this.columnType = columnType;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public boolean isByteData() {
        return byteData;
    }

    public void setByteData(boolean byteData) {
        this.byteData = byteData;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public ProxyMetadata copy() {
        var result = new ProxyMetadata(columnName, byteData);
        result.setPrecision(precision);
        result.setColumnType(columnType);
        return result;
    }
}
