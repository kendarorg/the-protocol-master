package org.kendar.plugins;

import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.utils.ChangeableReference;

import java.util.List;

public abstract class JdbcMockPlugin extends MockPlugin<JdbcCall, SelectResult> {
    private final SqlStringParser parser;

    public JdbcMockPlugin() {
        super();
        parser = new SqlStringParser(getSqlStringParserSeparator());
    }
    @Override
    protected void checkMatching(MockStorage data, JdbcCall requestToSimulate, ChangeableReference<Integer> matchingQuery, ChangeableReference<Long> foundedIndex) {
        var possibleRequest = data.retrieveInAs(JdbcCall.class);
        var possibleTokenized = parser.parseString(possibleRequest.getQuery());
        var requestedTokenized = parser.parseString(requestToSimulate.getQuery());
        asdfasd
    }

    protected abstract String getSqlStringParserSeparator();

    @Override
    protected void writeOutput(JdbcCall request, SelectResult response, MockStorage founded) {

    }

    @Override
    protected List<MockStorage> firstCheckOnMainPart(JdbcCall request) {
        return List.of();
    }
}
