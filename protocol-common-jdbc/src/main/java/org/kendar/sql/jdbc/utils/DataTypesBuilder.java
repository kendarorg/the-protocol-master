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
        clazz = switch (type) {
            case Types.STRUCT -> Struct.class;
            case Types.ARRAY -> Array.class;
            case Types.BIT, Types.BOOLEAN -> Boolean.class;
            case Types.SQLXML -> SQLXML.class;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> byte[].class;
            case Types.BLOB -> Blob.class;
            case Types.SMALLINT -> Short.class;
            case Types.TINYINT -> Byte.class;
            case Types.INTEGER -> Integer.class;
            case Types.BIGINT -> Long.class;
            case Types.REAL -> Float.class;
            case Types.DOUBLE -> Double.class;
            case Types.DATE -> java.sql.Date.class;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> Time.class;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> Timestamp.class;
            case Types.NUMERIC, Types.DECIMAL -> BigDecimal.class;
            case Types.CLOB -> Clob.class;
            case Types.NULL, Types.CHAR, Types.VARCHAR, Types.NVARCHAR, Types.NCLOB, Types.NCHAR, Types.LONGNVARCHAR,
                 Types.LONGVARCHAR -> String.class;
            default -> Object.class;
        };
        return clazz.getName();
    }

    public DataTypesBuilder ignoring(String... ignorables) {
        this.ignorables.addAll(Arrays.asList(ignorables));
        return this;
    }


}
