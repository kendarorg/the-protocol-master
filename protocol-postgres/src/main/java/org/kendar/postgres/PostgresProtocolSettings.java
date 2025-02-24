package org.kendar.postgres;

import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

@TpmService(tags = "postgres")
public class PostgresProtocolSettings extends JdbcProtocolSettings {
    public PostgresProtocolSettings() {
        setProtocol("postgres");
    }
}
