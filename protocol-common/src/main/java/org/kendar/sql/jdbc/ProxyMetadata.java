package org.kendar.sql.jdbc;

import java.sql.JDBCType;

public class ProxyMetadata {
    private String columnName;
    private String columnLabel;
    private boolean byteData;
    private String catalogName;
    private String schemaName;
    private String tableName;
    private int columnDisplaySize;
    private JDBCType columnType;
    private int precision;

    public ProxyMetadata() {

    }

    public ProxyMetadata(String columnName, boolean byteData) {
        this.columnName = columnName;
        this.byteData = byteData;
        this.columnLabel = columnName;
    }

    public ProxyMetadata(String columnName, String columnLabel, boolean byteData, String catalogName, String schemaName, String tableName, int columnDisplaySize, int columnType, int precision) {

        this.columnName = columnName;
        this.columnLabel = columnLabel;
        if (columnLabel == null || columnLabel.isEmpty()) {
            this.columnLabel = columnName;
        }
        this.byteData = byteData;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columnDisplaySize = columnDisplaySize;
        this.columnType = JDBCType.valueOf(columnType);
        this.precision = precision;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getColumnDisplaySize() {
        return columnDisplaySize;
    }

    public void setColumnDisplaySize(int columnDisplaySize) {
        this.columnDisplaySize = columnDisplaySize;
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

    public String getColumnLabel() {
        return columnLabel;
    }

    public void setColumnLabel(String columnLabel) {
        this.columnLabel = columnLabel;
    }
}
