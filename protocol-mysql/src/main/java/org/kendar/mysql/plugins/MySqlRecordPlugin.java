package org.kendar.mysql.plugins;

import org.kendar.filters.BasicJdbcRecordingPlugin;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

import java.util.HashMap;
import java.util.Map;

public class MySqlRecordPlugin extends BasicJdbcRecordingPlugin {
    private static final String SELECT_TRANS = "SELECT @@session.transaction_read_only";
    @Override
    public String getProtocol() {
        return "mysql";
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var data = new HashMap<String, String>();
        var input = (JdbcRequest) item.getInput();
        data.put("query", null);
        data.put("isIntResult", "true");
        data.put("parametersCount", "0");
        data.put("resultsCount", "0");
        if (item.getInput() != null) {
            if (input.getQuery() != null) {
                data.put("query", input.getQuery());
            }
            if (input.getParameterValues() != null) {
                data.put("parametersCount", input.getParameterValues().size() + "");

            }
        }
        if (item.getOutput() != null) {
            var output = (JdbcResponse) item.getOutput();
            data.put("isIntResult", Boolean.toString(output.getSelectResult().isIntResult()));
            if (output.getSelectResult().isIntResult()) {
                data.put("resultsCount", output.getIntResult() + "");
            } else {
                data.put("resultsCount", output.getSelectResult().getCount() + "");
            }
        }
        return data;
    }

    @Override
    protected boolean shouldNotSave(StorageItem item, CompactLine cl) {
        var shouldNotSave = super.shouldNotSave(item, cl);
        if (cl.getTags().get("query").toUpperCase().startsWith(SELECT_TRANS.toUpperCase())) {
            if (item != null) {
                cl.getTags().put("isIntResult", "true");
                cl.getTags().put("resultsCount", ((JdbcResponse)item.getOutput()).getSelectResult().getRecords().get(0).get(0));
            }
            return true;
        }
        return shouldNotSave;
    }
}

