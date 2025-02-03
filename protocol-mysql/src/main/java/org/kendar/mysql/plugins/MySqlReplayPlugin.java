package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcReplayPlugin;
import org.kendar.sql.jdbc.ProxyMetadata;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.sql.Types;

@TpmService(tags = "mysql")
public class MySqlReplayPlugin extends JdbcReplayPlugin {
    private static final String SELECT_TRANS = "SELECT @@session.transaction_read_only";
    private static final String SELECT_TRANS_RESULT = """
            {
                  "records" : [ [ "0" ] ],
                  "metadata" : [ {
                    "columnName" : "@@session.transaction_read_only",
                    "columnLabel" : "@@session.transaction_read_only",
                    "byteData" : false,
                    "catalogName" : "",
                    "schemaName" : "",
                    "tableName" : "",
                    "columnDisplaySize" : 19,
                    "columnType" : "BIGINT",
                    "precision" : 19
                  } ],
                  "count" : 1,
                  "intResult" : false,
                  "lastInsertedId" : 0
                }""";
    private static final SqlStringParser parser = new SqlStringParser("?");

    public MySqlReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    protected LineToRead beforeSendingReadResult(LineToRead lineToRead) {
        if (lineToRead == null) return null;
        lineToRead = super.beforeSendingReadResult(lineToRead);
        var idx = lineToRead.getCompactLine();
        var si = lineToRead.getStorageItem();
        if (si != null) {
            var jdbcRequest = (JdbcRequest) si.retrieveInAs(JdbcRequest.class);
            if (jdbcRequest != null) {
                if (jdbcRequest.getQuery().toUpperCase().startsWith(SELECT_TRANS.toUpperCase())) {
                    var jdbcResponse = (JdbcResponse) si.retrieveOutAs(JdbcResponse.class);
                    var selectResult = new JsonMapper().deserialize(SELECT_TRANS_RESULT, SelectResult.class);
                    var realResultValue = jdbcResponse.getIntResult();
                    selectResult.getRecords().get(0).set(0, realResultValue + "");
                    jdbcResponse.setSelectResult(selectResult);
                    jdbcResponse.setIntResult(0);
                    si.setInput(jdbcRequest);
                    si.setOutput(jdbcResponse);

                }
                return lineToRead;
            }
        }
        if (idx != null) {
            JdbcResponse resp = new JdbcResponse();
            if (idx.getTags().get("query").toUpperCase().startsWith(SELECT_TRANS.toUpperCase())) {
                var selectResult = new JsonMapper().deserialize(SELECT_TRANS_RESULT, SelectResult.class);
                var realResultValue = idx.getTags().get("resultsCount");
                selectResult.getRecords().get(0).set(0, realResultValue);
                resp.setSelectResult(selectResult);
                resp.setIntResult(0);
            } else if (idx.getTags().get("isIntResult").equalsIgnoreCase("true")) {
                resp.setIntResult(Integer.parseInt(idx.getTags().get("resultsCount")));
                SelectResult resultset = new SelectResult();
                resultset.setIntResult(true);
                resultset.setCount(resp.getIntResult());
                resp.setSelectResult(resultset);
            } else {
                resp.setIntResult(0);
                SelectResult resultset = new SelectResult();
                resultset.setIntResult(false);
                resultset.setCount(0);
                resultset.getMetadata().add(new ProxyMetadata("test", false, Types.INTEGER, 11));
                resp.setSelectResult(resultset);
            }
            si.setOutput(resp);
            JdbcRequest req = new JdbcRequest();
            req.setQuery(idx.getTags().get("query"));
            si.setInput(req);
        }


        return lineToRead;
    }

    @Override
    protected SqlStringParser getParser() {
        return parser;
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
