package org.kendar.plugins;

import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.utils.ChangeableReference;

import java.util.List;
import java.util.regex.Pattern;

public abstract class JdbcMockPlugin extends MockPlugin<JdbcCall, SelectResult> {
    private final SqlStringParser parser;

    public JdbcMockPlugin() {
        super();
        parser = new SqlStringParser(getSqlStringParserSeparator());
    }
    @Override
    protected void checkMatching(MockStorage data, JdbcCall callToSimulate, ChangeableReference<Integer> matchingQuery, ChangeableReference<Long> foundedIndex) {
        ChangeableReference<Integer> matchedQuery = new ChangeableReference<>(0);
        var requestToSimulate = new JdbcRequest();
        requestToSimulate.setQuery(callToSimulate.getQuery());
        requestToSimulate.setParameterValues(callToSimulate.getParameterValues());
        var possibleRequest = data.retrieveInAs(JdbcRequest.class);
        if(possibleRequest.getParameterValues().size()!=requestToSimulate.getParameterValues().size()){
            return;
        }
        if (!verifyQuery(requestToSimulate, possibleRequest, matchedQuery)) {
            return;
        }
        matchParameters(possibleRequest.getParameterValues(), requestToSimulate.getParameterValues(), matchedQuery);

        if (matchedQuery.get() > matchingQuery.get()) {
            matchingQuery.set(matchedQuery.get());
            foundedIndex.set(data.getIndex());
        }
    }

    private void matchParameters(List<BindingParameter> possibleMatches,
                                 List<BindingParameter> requestsToMatch,
                                 ChangeableReference<Integer> matchedQuery) {

        for (int i = 0; i < possibleMatches.size(); i++) {
            var possibleMatch = possibleMatches.get(i);
            var requestToMatch = requestsToMatch.get(i);
            if (possibleMatch.getValue()==null && requestToMatch.getValue()==null) {
                matchedQuery.set(3 + matchedQuery.get());
            }else if (possibleMatch.getValue()==null && requestToMatch.getValue()!=null) {
                //DO NOTHING
            }else if (possibleMatch.getValue().equalsIgnoreCase(requestToMatch.getValue())) {
                matchedQuery.set(3 + matchedQuery.get());
            } else if (possibleMatch.getValue().startsWith("${") && possibleMatch.getValue().endsWith("}")) {
                matchedQuery.set(1 + matchedQuery.get());
            } else if (possibleMatch.getValue().startsWith("@{") && possibleMatch.getValue().endsWith("}")) {
                var regexp = possibleMatch.getValue().substring(2, possibleMatch.getValue().length() - 3);
                if (Pattern.compile(regexp).matcher(requestToMatch.getValue()).matches()) {
                    matchedQuery.set(2 + matchedQuery.get());
                }
            }
        }
    }

    private boolean verifyQuery(JdbcRequest requestToSimulate, JdbcRequest possibleRequest, ChangeableReference<Integer> matchedQuery) {
        if (possibleRequest.getQuery().equalsIgnoreCase(requestToSimulate.getQuery())) {
            matchedQuery.set(10000 + matchedQuery.get());
            return true;
        }
        var possiblePath = parser.parseString(possibleRequest.getQuery());
        var toSimulatePath = parser.parseString(requestToSimulate.getQuery());
        if (possiblePath.size() != toSimulatePath.size()) {
            return false;
        }
        for (var i = 0; i < possiblePath.size(); i++) {
            var poss = possiblePath.get(i);
            var toss = toSimulatePath.get(i);
            if (!poss.equalsIgnoreCase(toss) && !areQuerySectionsSimilar(poss, toss)) {
                return false;
            }
        }
        matchedQuery.set(1000 + matchedQuery.get());
        return true;
    }

    private boolean areQuerySectionsSimilar(String possiblePathPart, String toMatchPathPart) {
        if (possiblePathPart.startsWith("${") && possiblePathPart.endsWith("}")) {
            return true;
        } else if (possiblePathPart.startsWith("@{") && possiblePathPart.endsWith("}")) {
            var regexp = possiblePathPart.substring(2, possiblePathPart.length() - 3);
            if (Pattern.compile(regexp).matcher(toMatchPathPart).matches()) {
                return true;
            }
        }
        return false;
    }

    protected abstract String getSqlStringParserSeparator();

    @Override
    protected void writeOutput(JdbcCall request, SelectResult response, MockStorage founded) {
        var foundedSelectResult = founded.retrieveOutAs(JdbcResponse.class);
        response.fill(foundedSelectResult.getSelectResult());
    }

    @Override
    protected List<MockStorage> firstCheckOnMainPart(JdbcCall request) {
        return mocks;
    }
}
