package org.kendar.sql.jdbc.settings;

import org.kendar.settings.ByteProtocolSettingsWithLogin;

public class JdbcProtocolSettings extends ByteProtocolSettingsWithLogin {
    private String forceSchema;
    private String replaceQueryFile;

    public String getReplaceQueryFile() {
        return replaceQueryFile;
    }

    public void setReplaceQueryFile(String replaceQueryFile) {
        this.replaceQueryFile = replaceQueryFile;
    }

    public String getForceSchema() {
        return forceSchema;
    }

    public void setForceSchema(String forceSchema) {
        this.forceSchema = forceSchema;
    }
}
