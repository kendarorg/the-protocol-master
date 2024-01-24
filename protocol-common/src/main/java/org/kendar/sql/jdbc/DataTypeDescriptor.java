package org.kendar.sql.jdbc;

import java.sql.JDBCType;
import java.util.Objects;

public class DataTypeDescriptor {
    private String name;
    private int dataType;
    private int precision;
    private int sqlDataType;
    private int sqlDateTimeSub;
    private int numPrecRadix;
    private int dbSpecificId;
    private String className;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataTypeDescriptor that = (DataTypeDescriptor) o;
        return dataType == that.dataType && precision == that.precision && sqlDataType == that.sqlDataType && sqlDateTimeSub == that.sqlDateTimeSub && numPrecRadix == that.numPrecRadix && dbSpecificId == that.dbSpecificId && Objects.equals(name, that.name) && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataType, precision, sqlDataType, sqlDateTimeSub, numPrecRadix, dbSpecificId, className);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDataType() {
        return dataType;
    }

    public JDBCType extractJdbcType(){
        return JDBCType.valueOf(dataType);
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getSqlDataType() {
        return sqlDataType;
    }

    public void setSqlDataType(int sqlDataType) {
        this.sqlDataType = sqlDataType;
    }

    public int getSqlDateTimeSub() {
        return sqlDateTimeSub;
    }

    public void setSqlDateTimeSub(int sqlDateTimeSub) {
        this.sqlDateTimeSub = sqlDateTimeSub;
    }

    public int getNumPrecRadix() {
        return numPrecRadix;
    }

    public void setNumPrecRadix(int numPrecRadix) {
        this.numPrecRadix = numPrecRadix;
    }

    public int getDbSpecificId() {
        return dbSpecificId;
    }

    public void setDbSpecificId(int dbSpecificId) {
        this.dbSpecificId = dbSpecificId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return "[" + name + " " + className + " " + dataType + "]";
    }
}
