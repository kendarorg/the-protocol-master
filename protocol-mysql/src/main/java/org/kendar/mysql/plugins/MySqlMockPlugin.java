package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcMockPlugin;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlMockPlugin extends JdbcMockPlugin {
    public MySqlMockPlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper, repository);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
