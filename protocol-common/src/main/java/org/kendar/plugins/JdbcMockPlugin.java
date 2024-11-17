package org.kendar.plugins;

import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.utils.ChangeableReference;

import java.util.List;

public abstract class JdbcMockPlugin extends MockPlugin<JdbcCall, SelectResult> {
    @Override
    protected void checkMatching(MockStorage data, JdbcCall requestToSimulate, ChangeableReference<Integer> matchingQuery, ChangeableReference<Long> foundedIndex) {

    }

    @Override
    protected void writeOutput(JdbcCall request, SelectResult response, MockStorage founded) {

    }

    @Override
    protected List<MockStorage> firstCheckOnMainPart(JdbcCall request) {
        return List.of();
    }
}
