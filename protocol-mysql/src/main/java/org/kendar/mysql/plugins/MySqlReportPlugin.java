package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcReportPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlReportPlugin extends JdbcReportPlugin {
    public MySqlReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
