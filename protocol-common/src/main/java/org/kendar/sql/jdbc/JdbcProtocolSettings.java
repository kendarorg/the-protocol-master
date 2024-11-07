package org.kendar.sql.jdbc;

import org.kendar.settings.ByteProtocolSettingsWithLogin;

public class JdbcProtocolSettings extends ByteProtocolSettingsWithLogin {
    private String forceSchema;

    public String getForceSchema() {
        return forceSchema;
    }

    public void setForceSchema(String forceSchema) {
        this.forceSchema = forceSchema;
    }
}
