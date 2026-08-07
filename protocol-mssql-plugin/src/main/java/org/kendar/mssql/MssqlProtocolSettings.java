package org.kendar.mssql;

import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

@Extension
@TpmService(tags = "mssql")
public class MssqlProtocolSettings extends JdbcProtocolSettings implements ExtensionPoint {
    public MssqlProtocolSettings() {
        setProtocol("mssql");
    }
}
