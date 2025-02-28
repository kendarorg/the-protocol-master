package org.kendar.postgres.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public PostgresLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }

}
