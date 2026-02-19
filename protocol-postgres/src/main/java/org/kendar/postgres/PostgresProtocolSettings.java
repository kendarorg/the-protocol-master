package org.kendar.postgres;

import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

@TpmService(tags = "postgres")
public class PostgresProtocolSettings extends JdbcProtocolSettings {

    private boolean useTls = false;
    public PostgresProtocolSettings() {
        setProtocol("postgres");
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }
}
