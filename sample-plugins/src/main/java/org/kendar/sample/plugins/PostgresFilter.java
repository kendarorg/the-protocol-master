package org.kendar.sample.plugins;

import org.kendar.annotations.di.TpmService;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

import java.util.List;

@Extension
@TpmService(tags = "postgres")
public class PostgresFilter extends ProtocolPluginDescriptorBase<PostgresFilterSettings> implements AlwaysActivePlugin {
    public PostgresFilter(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, JdbcCall in, SelectResult out) {
        return false;
    }

    @Override
    public Class<?> getSettingClass() {
        return PostgresFilterSettings.class;
    }

    /**
     * Only PRE_CALL and POST_CALL for things different from http
     *
     * @return
     */
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of();
    }

    @Override
    public String getId() {
        return "sample-postgres";
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }


}
