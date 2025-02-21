package org.kendar.postgres.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcRewritePlugin;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresRewritePlugin extends JdbcRewritePlugin {
    public PostgresRewritePlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper, repository);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
