package org.kendar.mysql.plugins;

import org.kendar.plugins.JdbcRecordingPlugin;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

import java.util.HashMap;
import java.util.Map;

public class MySqlRecordPlugin extends JdbcRecordingPlugin {
    private static final String SELECT_TRANS = "SELECT @@session.transaction_read_only";

    @Override
    public String getProtocol() {
        return "mysql";
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var data = new HashMap<String, String>();
        var input = (JdbcRequest) item.retrieveInAs(JdbcRequest.class);
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
            var output = (JdbcResponse) item.retrieveOutAs(JdbcResponse.class);
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
    protected boolean shouldNotSaveJdbc(StorageItem in, CompactLine cl) {
        var shouldNotSave = super.shouldNotSaveJdbc(in, cl);
        var result = in.retrieveInAs(JdbcRequest.class);
        if (result.getQuery().trim().toLowerCase().startsWith("show")) {
            cl.getTags().put("isIntResult", "false");
            cl.getTags().put("resultsCount", "0");
            return true;
        }
        if (result.getQuery().trim().toLowerCase().startsWith("use")) {
            cl.getTags().put("isIntResult", "true");
            cl.getTags().put("resultsCount", "0");
            return true;
        }
        if (cl.getTags().get("query").toUpperCase().startsWith(SELECT_TRANS.toUpperCase())) {
            if (in != null) {
                cl.getTags().put("isIntResult", "true");
                cl.getTags().put("resultsCount", ((JdbcResponse) in.getOutput()).getSelectResult().getRecords().get(0).get(0));
            }
            return true;
        }
        return shouldNotSave;
    }
}

