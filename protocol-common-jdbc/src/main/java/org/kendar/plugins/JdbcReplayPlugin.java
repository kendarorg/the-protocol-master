package org.kendar.plugins;

import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.storage.generic.LineToRead;

public abstract class JdbcReplayPlugin extends ReplayPlugin<BasicReplayPluginSettings> {

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
