package org.kendar.plugins;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.ReplacerItemInstance;

import java.util.List;

public abstract class JdbcRewritePlugin extends RewritePlugin<JdbcCall, SelectResult, RewritePluginSettings, String> {
    public JdbcRewritePlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper,repository);
    }

    @Override
    protected void replaceData(ReplacerItemInstance item, String toReplace, JdbcCall request, SelectResult response) {
        var replaced = item.run(toReplace);
        //noinspection StringEquality
        if (replaced != toReplace) {
            request.setQuery(replaced);
        }
    }

    @Override
    protected Class<?> getIn() {
        return JdbcCall.class;
    }

    @Override
    protected Class<?> getOut() {
        return SelectResult.class;
    }

    @Override
    protected String prepare(JdbcCall request, SelectResult response) {
        return request.getQuery().replaceAll("\r\n", "\n").trim();
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }
}
