package org.kendar.plugins;

import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.ExtraStringReplacer;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class JdbcMockPlugin extends MockPlugin<JdbcCall, SelectResult> {


    public JdbcMockPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    protected Class<?> getIn() {
        return JdbcCall.class;
    }

    @Override
    protected Class<?> getOut() {
        return SelectResult.class;
    }

    @Override
    protected void checkMatching(MockStorage data, JdbcCall callToSimulate, ChangeableReference<Integer> matchingQuery, ChangeableReference<Long> foundedIndex) {
        ChangeableReference<Integer> matchedQuery = new ChangeableReference<>(0);
        var requestToSimulate = new JdbcRequest();
        requestToSimulate.setQuery(callToSimulate.getQuery());
        requestToSimulate.setParameterValues(callToSimulate.getParameterValues());
        var possibleRequest = data.retrieveInAs(JdbcRequest.class);
        if (possibleRequest.getParameterValues().size() != requestToSimulate.getParameterValues().size()) {
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
            if (possibleMatch.getValue() == null && requestToMatch.getValue() == null) {
                matchedQuery.set(3 + matchedQuery.get());
            } else if (possibleMatch.getValue() == null && requestToMatch.getValue() != null) {
                //DO NOTHING
            } else if (possibleMatch.getValue().equalsIgnoreCase(requestToMatch.getValue())) {
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
        var possiblePath = ExtraStringReplacer.parse(possibleRequest.getQuery());
        var toSimulatePath = ExtraStringReplacer.match(possiblePath, requestToSimulate.getQuery());
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
            return Pattern.compile(regexp).matcher(toMatchPathPart).matches();
        }
        return false;
    }

    @Override
    protected List<MockStorage> firstCheckOnMainPart(JdbcCall request) {
        return new ArrayList<>(mocks.values());
    }


    @Override
    protected void writeOutput(JdbcCall request, SelectResult response, MockStorage founded) {
        var foundedCloneResponse = founded.retrieveOutAs(JdbcResponse.class).copy();
        loadParameters(founded.retrieveInAs(JdbcRequest.class), request, foundedCloneResponse);
        response.fill(foundedCloneResponse.getSelectResult());
    }

    private void loadParameters(JdbcRequest foundedRequest, JdbcCall originalRequest, JdbcResponse clonedResponse) {
        var parameters = new HashMap<String, String>();
        loadQueryParameters(foundedRequest, originalRequest, parameters);
        loadParamsParameters(foundedRequest, originalRequest, parameters);
        writeSelectResultParameter(clonedResponse, parameters);
    }

    private void writeSelectResultParameter(JdbcResponse clonedResponse, HashMap<String, String> parameters) {
        for (var item : clonedResponse.getSelectResult().getRecords()) {
            for (int i = 0; i < item.size(); i++) {
                var subItem = item.get(i);
                for (var param : parameters.entrySet()) {
                    subItem = subItem.replaceAll(Pattern.quote(param.getKey()), Matcher.quoteReplacement(param.getValue()));
                }
                item.set(i, subItem);
            }
        }
    }

    private void loadParamsParameters(JdbcRequest foundedRequest, JdbcCall originalRequest, HashMap<String, String> parameters) {
        if (!foundedRequest.getParameterValues().isEmpty()) {
            List<BindingParameter> parameterValues = foundedRequest.getParameterValues();
            for (int i = 0; i < parameterValues.size(); i++) {
                var tplParam = parameterValues.get(i);
                var oriParam = originalRequest.getParameterValues().get(i);
                if (isTemplateParameter(tplParam.getValue())) {
                    parameters.put(tplParam.getValue(), oriParam.getValue());
                }
            }
        }
    }

    private void loadQueryParameters(JdbcRequest foundedRequest, JdbcCall originalRequest, HashMap<String, String> parameters) {
        var tplPath = ExtraStringReplacer.parse(foundedRequest.getQuery());
        var oriPath = ExtraStringReplacer.match(tplPath, originalRequest.getQuery());
        for (var i = 0; i < tplPath.size(); i++) {
            var tplSeg = tplPath.get(i);
            if (isTemplateParameter(tplSeg)) {
                var oriSeg = oriPath.get(i);
                parameters.put(tplSeg, oriSeg);
            } else if (tplSeg.length() > 2 && isTemplateParameter(tplSeg.substring(1, tplSeg.length() - 2))) {
                var oriSeg = oriPath.get(i);
                parameters.put(tplSeg, oriSeg);
            }
        }
    }
}
