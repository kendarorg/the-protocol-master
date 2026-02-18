package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicJdbcForwardPlugin;
import org.kendar.plugins.JdbcRewritePlugin;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlForwardPlugin extends BasicJdbcForwardPlugin {


    public MySqlForwardPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
