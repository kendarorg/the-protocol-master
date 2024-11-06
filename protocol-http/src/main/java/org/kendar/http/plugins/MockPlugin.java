package org.kendar.http.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.filters.ProtocolPhase;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.http.utils.MimeChecker;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.http.utils.constants.ConstantsHeader;
import org.kendar.http.utils.constants.ConstantsMime;
import org.kendar.http.utils.utils.Md5Tester;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MockPlugin extends ProtocolPluginDescriptor<Request, Response> {
    private static final Logger log = LoggerFactory.getLogger(MockPlugin.class);
    final JsonMapper mapper = new JsonMapper();
    final ConcurrentHashMap<Long, Long> calls = new ConcurrentHashMap<>();
    private final List<StorageItem<Request, Response>> items = new ArrayList<>();
    private final Map<Long, String> hashes = new HashMap();
    private Path repository;
    private TypeReference<StorageItem<Request, Response>> typeReference;
    private boolean blockExternal = true;
    private List<Pattern> matchSites;

    private static void writeHeaderParameter(Response response, HashMap<String, String> parameters) {
        for (var kvp : response.getHeaders().entrySet()) {
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

    private static void loadPathParameters(Request templateRequest, Request originalRequest, HashMap<String, String> parameters) {
        var tplPath = templateRequest.getPath().split("/");
        var oriPath = originalRequest.getPath().split("/");
        for (var i = 0; i < tplPath.length; i++) {
            var tplSeg = tplPath[i];
            if (isTemplateParameter(tplSeg)) {
                var oriSeg = oriPath[i];
                parameters.put(tplSeg, oriSeg);
            }
        }
    }

    private static boolean isTemplateParameter(String tplSeg) {
        return tplSeg.startsWith("${") && tplSeg.endsWith("}");
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "mock-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    private void setupSitesToRecord(String recordSites) {
        if (recordSites == null) recordSites = "";
        this.matchSites = List.of(recordSites.split(",")).stream()
                .map(s -> s.trim()).filter(s -> s.length() > 0)
                .map(s -> Pattern.compile(s)).collect(Collectors.toList());
    }

    @Override
    public void initialize(Map<String, Object> section) {
        typeReference = new TypeReference<>() {
        };
        var recordingPath = (String) section.get("datadir");
        setupSitesToRecord((String) section.get("matchSites"));
        blockExternal = Boolean.parseBoolean((String) section.get("blockExternal"));
        recordingPath = recordingPath.replace("{milliseconds}", Calendar.getInstance().getTimeInMillis() + "");
        repository = Path.of(recordingPath);

        if (!Files.exists(repository)) {
            try {
                Files.createDirectories(repository);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (var file : repository.toFile().listFiles()) {
            if (file.isDirectory()) continue;
            if (file.getName().endsWith(".json") && !file.getName().endsWith("index.json")) {
                try {
                    var item = mapper.deserialize(Files.readString(file.toPath()), typeReference);
                    items.add(item);
                    String contentHash;
                    contentHash = Md5Tester.calculateMd5(item.getInput().getRequestText());
                    hashes.put(item.getIndex(), contentHash);
                } catch (IOException e) {
                    log.error("ERROR ", e);
                }
            }
        }
    }

    @Override
    public boolean handle(ProtocolPhase phase, Request request, Response response) {
        if (matchSites.size() > 0) {
            var matchFound = false;
            for (var pat : matchSites) {
                if (pat.matcher(request.getHost()).matches()) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        String contentHash = Md5Tester.calculateMd5(request.getRequestText());
        if (findMatch(request, response, contentHash)) {


            log.info("REP " + request.getMethod() + " " + request.buildUrl());
            return true;
        }
        if (blockExternal) {
            response.setStatusCode(404);
            response.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.TEXT);
            response.setResponseText("Page Not Found: " + request.getMethod() + " on " + request.buildUrl());
            return true;
        }
        return false;
    }

    @Override
    public void terminate() {

    }

    private boolean findMatch(Request request, Response response, String contentHash) {

        var matchingQuery = new AtomicInteger(0);
        var foundedIndex = new AtomicLong(-1);
        var withHost = items.stream().filter(a -> a.getInput().getHost().equalsIgnoreCase(request.getHost())).collect(Collectors.toList());
        var parametric = new AtomicBoolean(false);
        withHost.forEach(a -> checkMatching(a, request, contentHash, matchingQuery, foundedIndex, parametric));
        if (foundedIndex.get() <= 0) {
            parametric.set(true);
            withHost.forEach(a -> checkMatching(a, request, contentHash, matchingQuery, foundedIndex, parametric));
        }
        if (foundedIndex.get() > 0) {
            if (!parametric.get()) {
                if (calls.containsKey(foundedIndex.longValue())) {
                    return false;
                }
                calls.put(foundedIndex.longValue(), foundedIndex.longValue());
            }
            var foundedResponse = items.stream().filter(a -> a.getIndex() == foundedIndex.get()).findFirst();
            if (foundedResponse.isPresent()) {
                var founded = foundedResponse.get().getOutput();
                if (founded != null) {
                    var foundedClone = founded.copy();
                    if (parametric.get()) {
                        loadParameters(foundedResponse.get().getInput(), request, foundedClone);
                    }
                    response.setResponseText(foundedClone.getResponseText());
                    response.setHeaders(foundedClone.getHeaders());
                    response.setStatusCode(foundedClone.getStatusCode());
                    response.setMessages(foundedClone.getMessages());
                }
            }
            return true;
        }
        return false;
    }

    private void checkMatching(StorageItem<Request, Response> data,
                               Request requestToSimulate,
                               String requestToSimulateContentHash,
                               AtomicInteger matchingQuery,
                               AtomicLong foundedIndex,
                               AtomicBoolean parametric) {
        AtomicInteger matchedQuery = new AtomicInteger(0);

        var possibleRequest = data.getInput();
        verifyHostAndPath(requestToSimulate, possibleRequest, matchedQuery, parametric);
        if (matchedQuery.get() == 0) return;

        matchContent(data, requestToSimulate, requestToSimulateContentHash, possibleRequest, matchedQuery);
        matchQuery(possibleRequest.getQuery(), requestToSimulate.getQuery(), matchedQuery, parametric);

        if (matchedQuery.get() > matchingQuery.get()) {
            matchingQuery.set(matchedQuery.get());
            foundedIndex.set(data.getIndex());
        }
    }

    private void matchContent(StorageItem<Request, Response> data, Request requestToSimulate, String requestToSimulateContentHash, Request possibleRequest, AtomicInteger matchedQuery) {
        var possibleContentHash = hashes.get(data.getIndex());

        if (MimeChecker.isBinary(possibleRequest) == MimeChecker.isBinary(requestToSimulate)) {
            if (requestToSimulateContentHash.equalsIgnoreCase(possibleContentHash)) {
                matchedQuery.addAndGet(20);
            }
        }
    }

    private void verifyHostAndPath(Request requestToSimulate, Request possibleRequest,
                                   AtomicInteger matchedQuery, AtomicBoolean parametric) {
        if (!possibleRequest.getHost().equalsIgnoreCase(requestToSimulate.getHost())) {
            return;
        }
        var possiblePath = possibleRequest.getPath().split("/");
        var toSimulatePath = requestToSimulate.getPath().split("/");
        if (possiblePath.length != toSimulatePath.length) {
            return;
        }

        for (var i = 0; i < possiblePath.length; i++) {
            var poss = possiblePath[i];
            var toss = toSimulatePath[i];
            if (poss.equalsIgnoreCase(toss)) {
                matchedQuery.addAndGet(3);
            } else if (arePathSectionsSimilar(poss, toss, parametric)) {
                matchedQuery.addAndGet(1);
            }
        }
        if (possiblePath.length == 0) {
            matchedQuery.addAndGet(1);
        }
    }

    private boolean arePathSectionsSimilar(String poss, String toss, AtomicBoolean parametric) {
        if (poss.startsWith("${") && poss.endsWith("}") && parametric.get()) {
            return true;
        }
        var possSpl = poss.split("=");
        var tossSpl = toss.split("=");
        if (possSpl.length != tossSpl.length) {
            return false;
        }
        if (possSpl.length > 1) {
            return possSpl[0].equalsIgnoreCase(tossSpl[0]);
        }
        return false;
    }

    private void matchQuery(Map<String, String> left, Map<String, String> right, AtomicInteger matchedQuery,
                            AtomicBoolean parametric) {
        for (var leftItem : left.entrySet()) {
            for (var rightItem : right.entrySet()) {
                if (leftItem.getKey().equalsIgnoreCase(rightItem.getKey())) {
                    matchedQuery.incrementAndGet();
                    if (leftItem.getValue() == null) {
                        //To ensure left!=null on else branch
                        if (rightItem.getValue() == null) {
                            matchedQuery.incrementAndGet();
                        }
                    } else if (leftItem.getValue().equalsIgnoreCase(rightItem.getValue())) {
                        matchedQuery.incrementAndGet();
                    } else if (parametric.get() && leftItem.getValue().startsWith("${") && leftItem.getValue().endsWith("}")) {
                        matchedQuery.incrementAndGet();
                    }
                }
            }
        }
    }

    private void loadParameters(Request templateRequest, Request originalRequest, Response response) {
        var parameters = new HashMap<String, String>();
        loadPathParameters(templateRequest, originalRequest, parameters);
        loadQueryParameters(templateRequest, originalRequest, parameters);
        loadHeaderParameters(templateRequest, originalRequest, parameters);
        writeHeaderParameter(response, parameters);
        writeDataParameter(response, parameters);
    }

    private void writeDataParameter(Response response, HashMap<String, String> parameters) {
        if (!MimeChecker.isBinary(response)) {
            var text = response.getResponseText();
            for (var param : parameters.entrySet()) {
                text = text.replaceAll(Pattern.quote(param.getKey()), param.getValue());
            }
        }
    }

    private void loadHeaderParameters(Request templateRequest, Request originalRequest, HashMap<String, String> parameters) {
        for (var kvp : templateRequest.getHeaders().entrySet()) {
            if (isTemplateParameter(kvp.getValue().get(0))) {
                if (originalRequest.getHeaders().containsKey(kvp.getKey())) {
                    parameters.put(kvp.getKey(), originalRequest.getHeaders().get(kvp.getKey()).get(0));
                }
            }
        }
    }

    private void loadQueryParameters(Request templateRequest, Request originalRequest, HashMap<String, String> parameters) {
        for (var kvp : templateRequest.getQuery().entrySet()) {
            if (isTemplateParameter(kvp.getKey())) {
                if (originalRequest.getQuery().containsKey(kvp.getKey())) {
                    parameters.put(kvp.getKey(), originalRequest.getQuery().get(kvp.getKey()));
                }
            }
        }
    }
}
