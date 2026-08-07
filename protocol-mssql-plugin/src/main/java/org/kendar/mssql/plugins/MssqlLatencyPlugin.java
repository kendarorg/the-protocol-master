package org.kendar.mssql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mssql")
public class MssqlLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public MssqlLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mssql";
    }
}
