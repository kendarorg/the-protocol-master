package org.kendar.mssql;

import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

@TpmService(tags = "mssql")
public class MssqlProtocolSettings extends JdbcProtocolSettings {
    public MssqlProtocolSettings() {
        setProtocol("mssql");
    }
}
