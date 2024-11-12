package org.kendar.mysql.plugins;

import org.kendar.filters.BasicJdbcReplayingPlugin;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.storage.generic.LineToRead;
import org.kendar.utils.JsonMapper;

public class MySqlReplayPlugin extends BasicJdbcReplayingPlugin {
    private static final String SELECT_TRANS = "SELECT @@session.transaction_read_only";
    private static final String SELECT_TRANS_RESULT = "{\n" +
            "      \"records\" : [ [ \"0\" ] ],\n" +
            "      \"metadata\" : [ {\n" +
            "        \"columnName\" : \"@@session.transaction_read_only\",\n" +
            "        \"columnLabel\" : \"@@session.transaction_read_only\",\n" +
            "        \"byteData\" : false,\n" +
            "        \"catalogName\" : \"\",\n" +
            "        \"schemaName\" : \"\",\n" +
            "        \"tableName\" : \"\",\n" +
            "        \"columnDisplaySize\" : 19,\n" +
            "        \"columnType\" : \"BIGINT\",\n" +
            "        \"precision\" : 19\n" +
            "      } ],\n" +
            "      \"count\" : 1,\n" +
            "      \"intResult\" : false,\n" +
            "      \"lastInsertedId\" : 0\n" +
            "    }";


    @Override
    protected LineToRead beforeSendingReadResult(LineToRead lineToRead) {
        if(lineToRead==null)return null;
        lineToRead = super.beforeSendingReadResult(lineToRead);
        var idx = lineToRead.getCompactLine();
        var si = lineToRead.getStorageItem();
        if(si!=null){
            var jdbcRequest = (JdbcRequest) si.retrieveInAs(JdbcRequest.class);
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
        if (idx != null) {
            JdbcResponse resp = new JdbcResponse();
            if (idx.getTags().get("isIntResult").equalsIgnoreCase("true")) {
                resp.setIntResult(Integer.parseInt(idx.getTags().get("resultsCount")));
                SelectResult resultset = new SelectResult();
                resultset.setIntResult(true);
                resultset.setCount(resp.getIntResult());
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
    public String getProtocol() {
        return "mysql";
    }
}
