package org.kendar.mssql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcReportPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mssql")
public class MssqlReportPlugin extends JdbcReportPlugin {
    public MssqlReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mssql";
    }
}
