package org.kendar.filters;

import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.pf4j.Extension;

import java.util.List;
import java.util.Map;

@Extension
public class PostgresFilter extends ProtocolPluginDescriptor<JdbcCall, SelectResult> implements AlwaysActivePlugin{
    @Override
    public boolean handle(ProtocolPhase phase, JdbcCall in, SelectResult out) {
        return false;
    }

    /**
     * Only PRE_CALL and POST_CALL for things different from http
     * @return
     */
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of();
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }

    @Override
    public void initialize(Map<String, Object> section) {

    }

    @Override
    public void terminate() {

    }
}
