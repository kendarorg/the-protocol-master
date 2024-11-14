package org.kendar.sql.jdbc.settings;

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
