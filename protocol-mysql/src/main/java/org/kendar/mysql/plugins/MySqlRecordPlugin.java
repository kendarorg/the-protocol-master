package org.kendar.mysql.plugins;

import org.kendar.plugins.JdbcRecordPlugin;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.dtos.TokenType;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MySqlRecordPlugin extends JdbcRecordPlugin {
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
                var tokenized = getParser().tokenize(input.getQuery()).stream().filter(a -> a.getType() != TokenType.VALUE_ITEM)
                        .map(t->t.getValue())
                        .collect(Collectors.toList());
                data.put("tokenized",String.join(" ",tokenized));
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

    private static final SqlStringParser parser = new SqlStringParser("?");

    @Override
    protected SqlStringParser getParser() {
        return parser;
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
            cl.getTags().put("isIntResult", "true");
            cl.getTags().put("resultsCount", ((JdbcResponse) in.getOutput()).getSelectResult().getRecords().get(0).get(0));
            return true;
        }
        return shouldNotSave;
    }
}

