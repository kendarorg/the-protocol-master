package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicMockPlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ChangeableReference;
import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TpmService(tags = "http")
public class HttpMockPlugin extends BasicMockPlugin<Request, Response> {

    public HttpMockPlugin(JsonMapper mapper, StorageRepository repository) {
        super(mapper, repository);
    }

    private static void writeHeaderParameter(Response clonedResponse, HashMap<String, String> parameters) {
        for (var kvp : clonedResponse.getHeaders().entrySet()) {
            List<String> value = kvp.getValue();
            for (int i = 0; i < value.size(); i++) {
                var v = value.get(i);
                if (isTemplateParameter(v)) {
                    value.clear();
                    value.add(parameters.get(v));
                    break;
                }
            }
        }
    }

    private static void loadPathParameters(Request foundedRequest, Request originalRequest, HashMap<String, String> parameters) {
        var tplPath = foundedRequest.getPath().split("/");
        var oriPath = originalRequest.getPath().split("/");
        for (var i = 0; i < tplPath.length; i++) {
            var tplSeg = tplPath[i];
            if (isTemplateParameter(tplSeg)) {
                var oriSeg = oriPath[i];
                parameters.put(tplSeg, oriSeg);
            }
        }
    }


    @Override
    public String getProtocol() {
        return "http";
    }


    @Override
    protected List<MockStorage> firstCheckOnMainPart(Request request) {
        return mocks.values().stream().filter(a -> a.retrieveInAs(Request.class).getHost().
                equalsIgnoreCase(request.getHost())).collect(Collectors.toList());
    }

    @Override
    protected void writeOutput(Request request, Response response, MockStorage founded) {
        var foundedCloneResponse = founded.retrieveOutAs(Response.class).copy();
        loadParameters(founded.retrieveInAs(Request.class), request, foundedCloneResponse);
        response.setResponseText(foundedCloneResponse.getResponseText());
        response.setHeaders(foundedCloneResponse.getHeaders());
        response.setStatusCode(foundedCloneResponse.getStatusCode());
        response.setMessages(foundedCloneResponse.getMessages());
    }

    @Override
    protected Class<?> getIn() {
        return Request.class;
    }

    @Override
    protected Class<?> getOut() {
        return Response.class;
    }

    @Override
    protected void checkMatching(MockStorage data,
                                 Request requestToSimulate,
                                 ChangeableReference<Integer> matchingQuery,
                                 ChangeableReference<Long> foundedIndex) {

        ChangeableReference<Integer> matchedQuery = new ChangeableReference<>(0);

        var possibleRequest = data.retrieveInAs(Request.class);
        if (!verifyHostAndPath(requestToSimulate, possibleRequest, matchedQuery)) {
            return;
        }
        matchQuery(possibleRequest.getQuery(), requestToSimulate.getQuery(), matchedQuery);

        if (matchedQuery.get() > matchingQuery.get()) {
            matchingQuery.set(matchedQuery.get());
            foundedIndex.set(data.getIndex());
        }
    }

    private boolean verifyHostAndPath(Request requestToSimulate,
                                      Request possibleRequest,
                                      ChangeableReference<Integer> matchedQuery) {
        if (!possibleRequest.getHost().equalsIgnoreCase(requestToSimulate.getHost())) {
            return false;
        }
        var possiblePath = possibleRequest.getPath().split("/");
        var toSimulatePath = requestToSimulate.getPath().split("/");
        if (possiblePath.length != toSimulatePath.length) {
            return false;
        }

        if (possibleRequest.getPath().equalsIgnoreCase(requestToSimulate.getPath())) {
            matchedQuery.set(10000 + matchedQuery.get());
            return true;
        }

        for (var i = 0; i < possiblePath.length; i++) {
            var poss = possiblePath[i];
            var toss = toSimulatePath[i];
            if (!poss.equalsIgnoreCase(toss) && !arePathSectionsSimilar(poss, toss)) {
                return false;
            }
        }
        matchedQuery.set(1000 + matchedQuery.get());
        return true;
    }

    private boolean arePathSectionsSimilar(String possiblePathPart, String toMatchPathPart) {
        if (possiblePathPart.startsWith("${") && possiblePathPart.endsWith("}")) {
            return true;
        } else if (possiblePathPart.startsWith("@{") && possiblePathPart.endsWith("}")) {
            var regexp = possiblePathPart.substring(2, possiblePathPart.length() - 3);
            if (Pattern.compile(regexp).matcher(toMatchPathPart).matches()) {
                return true;
            }
        }
        var possSpl = possiblePathPart.split("=");
        var tossSpl = toMatchPathPart.split("=");
        if (possSpl.length != tossSpl.length) {
            return false;
        }
        if (possSpl.length > 1) {
            return possSpl[0].equalsIgnoreCase(tossSpl[0]);
        }
        return false;
    }

    private void matchQuery(Map<String, String> possibleMatches,
                            Map<String, String> requestsToMatch,
                            ChangeableReference<Integer> matchedQuery) {
        for (var possibleMatch : possibleMatches.entrySet()) {
            for (var requestToMatch : requestsToMatch.entrySet()) {
                if (possibleMatch.getKey().equalsIgnoreCase(requestToMatch.getKey())) {
                    matchedQuery.set(1 + matchedQuery.get());
                    if (possibleMatch.getValue() == null) {
                        //To ensure left!=null on else branch
                        if (requestToMatch.getValue() == null) {
                            matchedQuery.set(1 + matchedQuery.get());
                        }
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
        }
    }

    /**
     * The parameters that are matching with "variables" in the founded request,
     * are collected, and set as variable inside the parameters map
     * After that they are all applied to the cloned response
     *
     * @param foundedRequest
     * @param originalRequest
     * @param clonedResponse
     */
    private void loadParameters(Request foundedRequest, Request originalRequest, Response clonedResponse) {
        var parameters = new HashMap<String, String>();
        loadPathParameters(foundedRequest, originalRequest, parameters);
        loadQueryParameters(foundedRequest, originalRequest, parameters);
        loadHeaderParameters(foundedRequest, originalRequest, parameters);
        writeHeaderParameter(clonedResponse, parameters);
        writeDataParameter(clonedResponse, parameters);
    }

    private void writeDataParameter(Response clonedResponse, HashMap<String, String> parameters) {
        if (!MimeChecker.isBinary(clonedResponse)) {
            var text = clonedResponse.getResponseText().asText();
            for (var param : parameters.entrySet()) {
                text = text.replaceAll(Pattern.quote(param.getKey()), Matcher.quoteReplacement(param.getValue()));
            }
            clonedResponse.setResponseText(new TextNode(text));
        }
    }

    private void loadHeaderParameters(Request foundedRequest, Request originalRequest, HashMap<String, String> parameters) {
        for (var kvp : foundedRequest.getHeaders().entrySet()) {
            if (isTemplateParameter(kvp.getValue().get(0))) {
                if (originalRequest.getFirstHeader(kvp.getKey()) != null) {
                    parameters.put(kvp.getValue().get(0), originalRequest.getFirstHeader(kvp.getKey()));
                }
            }
        }
    }

    private void loadQueryParameters(Request templateRequest, Request originalRequest, HashMap<String, String> parameters) {
        for (var kvp : templateRequest.getQuery().entrySet()) {
            if (isTemplateParameter(kvp.getValue())) {
                if (originalRequest.getQuery().containsKey(kvp.getKey())) {
                    parameters.put(kvp.getValue(), originalRequest.getQuery().get(kvp.getKey()));
                }
            }
        }
    }
}
