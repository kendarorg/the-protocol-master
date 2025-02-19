package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcRewritePlugin;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlRewritePlugin extends JdbcRewritePlugin {
    public MySqlRewritePlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper,repository);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
