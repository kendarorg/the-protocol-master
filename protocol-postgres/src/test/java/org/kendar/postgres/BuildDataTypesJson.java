package org.kendar.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import org.kendar.sql.jdbc.DataTypeDescriptor;
import org.kendar.sql.jdbc.utils.DataTypesBuilder;
import org.kendar.utils.JsonMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class BuildDataTypesJson extends PostgresBasicTest {
    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
    }

    private static ArrayList<DataTypeDescriptor> loadPostgresSpecificIds(Connection coco) {
        var resu = new ArrayList<DataTypeDescriptor>();
        try {

            var stmt = coco.prepareStatement("SELECT *" +
                    "FROM pg_catalog.pg_type t\n" +
                    "     LEFT JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace\n" +
                    "WHERE (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid))\n" +
                    "  AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid)\n" +
                    "  AND pg_catalog.pg_type_is_visible(t.oid)\n" +
                    "ORDER BY 1, 2;");
            var oidrs = stmt.executeQuery();
            while (oidrs.next()) {
                var dtd = new DataTypeDescriptor();
                var typeName = oidrs.getString("typname");
                var oid = oidrs.getInt("oid");
                dtd.setName(typeName);
                dtd.setDbSpecificId(oid);
                resu.add(dtd);
            }
        } catch (Exception ex) {
        }


        return resu;
    }

    //@Test
    void loadDbInfo() throws SQLException, ClassNotFoundException, JsonProcessingException {
        var c = getRealConnection();
        var dataTypesBuilder = new DataTypesBuilder(c)
                .setCustomIdLoader((coco) -> loadPostgresSpecificIds(coco))
                .setCustomBuilder((a, b) -> loadPostgresSpecificType(a, b))
                .ignoring("bpchar", "serial", "bigserial", "smallserial", "name", "oid");

        var result = dataTypesBuilder.run();
        JsonMapper mapper = new JsonMapper();
        var res = mapper.serialize(result);
        System.out.println(res);
    }

    private Boolean loadPostgresSpecificType(DataTypesBuilder dtb, DataTypeDescriptor dtd) {
        if ("json".equalsIgnoreCase(dtd.getName())) {
            dtd.setClassName("String");
            return true;
        } else if ("uuid".equalsIgnoreCase(dtd.getName())) {
            dtd.setClassName("UUID");
            return true;
        }
        return false;
    }


}
