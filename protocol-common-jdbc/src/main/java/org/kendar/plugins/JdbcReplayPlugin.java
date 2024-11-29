package org.kendar.plugins;

import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.dtos.TokenType;
import org.kendar.storage.generic.LineToRead;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class JdbcReplayPlugin extends ReplayPlugin<BasicReplayPluginSettings> {

    protected abstract SqlStringParser getParser();
    @Override
    protected Map<String, String> buildTag(Object in) {
        var jdbcCall = (JdbcCall) in;
        var result = new HashMap<String, String>();
        result.put("query", jdbcCall.getQuery());
        var tokenized = getParser().tokenize(jdbcCall.getQuery()).stream().filter(a -> a.getType() != TokenType.VALUE_ITEM)
                .map(t->t.getValue())
                .collect(Collectors.toList());
        result.put("tokenized",String.join(" ",tokenized));
        result.put("parametersCount", jdbcCall.getParameterValues().size() + "");
        return result;
    }
    public abstract String getProtocol();

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outputItem, Object aClass, LineToRead lineToRead) {
        var outObj = (SelectResult) aClass;
        var req = (JdbcCall) in;
        if (lineToRead != null && lineToRead.getStorageItem() != null
                && lineToRead.getStorageItem().getOutput() != null) {
            var source = lineToRead.getStorageItem().retrieveOutAs(JdbcResponse.class);
            outObj.fill(source.getSelectResult());
        } else if (lineToRead != null && lineToRead.getCompactLine() != null) {// if(in.getQuery().trim().toLowerCase().startsWith("set")){
            //completedIndexes.add((int) lineToRead.getCompactLine().getIndex());
            if (lineToRead.getCompactLine().getTags().get("isIntResult").equalsIgnoreCase("true")) {
                SelectResult resultset = new SelectResult();
                resultset.setIntResult(true);
                resultset.setCount(Integer.parseInt(lineToRead.getCompactLine().getTags().get("resultsCount")));
                outObj.fill(resultset);
            }
        }
    }
}
