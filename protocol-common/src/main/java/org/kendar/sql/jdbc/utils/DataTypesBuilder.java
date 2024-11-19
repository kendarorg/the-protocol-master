package org.kendar.sql.jdbc.utils;

import org.kendar.sql.jdbc.DataTypeDescriptor;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DataTypesBuilder {
    private final Connection connection;
    private final Set<String> ignorables = new HashSet<>();
    private Function<Connection, List<DataTypeDescriptor>> customIdLoader;
    private BiFunction<DataTypesBuilder, DataTypeDescriptor, Boolean> customBuilder;

    public DataTypesBuilder(Connection connection) {

        this.connection = connection;
    }

    public DataTypesBuilder setCustomIdLoader(Function<Connection, List<DataTypeDescriptor>> customIdLoader) {
        this.customIdLoader = customIdLoader;
        return this;
    }

    public DataTypesBuilder setCustomBuilder(BiFunction<DataTypesBuilder, DataTypeDescriptor, Boolean> customBuilder) {
        this.customBuilder = customBuilder;
        return this;
    }

    public List<DataTypeDescriptor> run() throws SQLException {


        var rs = connection.getMetaData().getTypeInfo();
        var allDtd = new ArrayList<DataTypeDescriptor>();
        while (rs.next()) {
            var dtd = new DataTypeDescriptor();
            dtd.setName(rs.getString("TYPE_NAME"));
            dtd.setDataType(rs.getInt("DATA_TYPE"));
            dtd.setPrecision(rs.getInt("PRECISION"));
            dtd.setSqlDataType(rs.getInt("SQL_DATA_TYPE"));
            dtd.setSqlDateTimeSub(rs.getInt("SQL_DATETIME_SUB"));
            dtd.setNumPrecRadix(rs.getInt("NUM_PREC_RADIX"));
            dtd.setClassName(getClName(dtd));
            if (
                    dtd.getDataType() != Types.OTHER
                            && !dtd.getName().contains("_")
                            && !"object".equalsIgnoreCase(dtd.getClassName())
                            && !this.ignorables.contains(dtd.getName())) {
                allDtd.add(dtd);
            } else if (customBuilder.apply(this, dtd)) {
                allDtd.add(dtd);
            }
        }
        var customItems = customIdLoader.apply(connection);
        for (var customItem : customItems) {
            var original = allDtd.stream().filter(a -> a.getName().equalsIgnoreCase(customItem.getName())).findFirst();
            original.ifPresent(dataTypeDescriptor -> dataTypeDescriptor.setDbSpecificId(customItem.getDbSpecificId()));
        }
        return allDtd;
    }

    @SuppressWarnings("rawtypes")
    private String getClName(DataTypeDescriptor dt) {
        Class clazz;
        var type = dt.getDataType();
        switch (type) {
            case Types.STRUCT:
                clazz = Struct.class;
                break;
            case Types.ARRAY:
                clazz = Array.class;
                break;
            case Types.BIT:
            case Types.BOOLEAN:
                clazz = Boolean.class;
                break;
            case Types.SQLXML:
                clazz = java.sql.SQLXML.class;
                break;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                clazz = byte[].class;
                break;
            case Types.BLOB:
                clazz = Blob.class;
                break;
            case Types.SMALLINT:
                clazz = Short.class;
                break;
            case Types.TINYINT:
                clazz = Byte.class;
                break;
            case Types.INTEGER:
                clazz = Integer.class;
                break;
            case Types.BIGINT:
                clazz = Long.class;
                break;
            case Types.REAL:
                clazz = Float.class;
                break;
            case Types.DOUBLE:
                clazz = Double.class;
                break;
            case Types.DATE:
                clazz = java.sql.Date.class;
                break;
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                clazz = java.sql.Time.class;
                break;
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                clazz = java.sql.Timestamp.class;
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                clazz = BigDecimal.class;
                break;
            case Types.CLOB:
                clazz = Clob.class;
                break;
            case Types.NULL:
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.NCLOB:
            case Types.NCHAR:
            case Types.LONGNVARCHAR:
            case Types.LONGVARCHAR:
                clazz = String.class;
                break;
            default:
                clazz = Object.class;
                break;
        }
        return clazz.getName();
    }

    public DataTypesBuilder ignoring(String... ignorables) {
        this.ignorables.addAll(Arrays.asList(ignorables));
        return this;
    }


}
