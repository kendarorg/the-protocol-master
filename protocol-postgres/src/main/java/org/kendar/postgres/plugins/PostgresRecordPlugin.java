package org.kendar.postgres.plugins;

import org.kendar.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcRecordPlugin;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.dtos.SimpleToken;
import org.kendar.sql.parser.dtos.TokenType;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@TpmService(tags = "postgres")
public class PostgresRecordPlugin extends JdbcRecordPlugin {
    private static final SqlStringParser parser = new SqlStringParser("$");

    @TpmConstructor
    public PostgresRecordPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    public String getProtocol() {
        return "postgres";
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
                        .map(SimpleToken::getValue)
                        .collect(Collectors.toList());
                data.put("tokenized", String.join(" ", tokenized));
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
    protected SqlStringParser getParser() {
        return parser;
    }
}
