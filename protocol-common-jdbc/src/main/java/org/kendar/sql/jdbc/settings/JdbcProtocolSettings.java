package org.kendar.sql.jdbc.settings;

import org.kendar.settings.ByteProtocolSettingsWithLogin;

public class JdbcProtocolSettings extends ByteProtocolSettingsWithLogin {
    private String forceSchema;
    private String driver;

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getForceSchema() {
        return forceSchema;
    }

    public void setForceSchema(String forceSchema) {
        this.forceSchema = forceSchema;
    }


}
