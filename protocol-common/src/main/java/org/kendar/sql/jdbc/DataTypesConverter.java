package org.kendar.sql.jdbc;

import java.sql.JDBCType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataTypesConverter {
    private final Map<Integer, DataTypeDescriptor> nativeToJdbc = new HashMap<>();
    private final Map<JDBCType, DataTypeDescriptor> jdbcToNative = new HashMap<>();

    public DataTypesConverter(List<DataTypeDescriptor> dtts) {

        for (var dtt : dtts) {
            nativeToJdbc.put(dtt.getDbSpecificId(), dtt);
            jdbcToNative.put(dtt.extractJdbcType(), dtt);
        }
    }

    public Optional<JDBCType> fromNativeToJdbc(int oid) {
        if (!nativeToJdbc.containsKey(oid)) return Optional.empty();
        return Optional.of(nativeToJdbc.get(oid).extractJdbcType());
    }

    public DataTypeDescriptor getDttFromNative(int oid) {
        return nativeToJdbc.get(oid);
    }

    public DataTypeDescriptor getDttFromJdbc(int oid) {
        return jdbcToNative.get(JDBCType.valueOf(oid));
    }

    public Class<?> getFromName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
