package org.kendar.postgres.plugins;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcMockPlugin;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresMockPlugin extends JdbcMockPlugin {


    @TpmConstructor
    public PostgresMockPlugin(JsonMapper mapper, StorageRepository repository, MultiTemplateEngine resolversFactory) {
        super(mapper, repository, resolversFactory);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
