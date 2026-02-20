package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicForwardPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlForwardPlugin extends BasicForwardPlugin {


    public MySqlForwardPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
