package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcRewritePlugin;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlRewritePlugin extends JdbcRewritePlugin {
    public MySqlRewritePlugin(JsonMapper mapper, StorageRepository repository, MultiTemplateEngine resolversFactory) {
        super(mapper, repository, resolversFactory);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
