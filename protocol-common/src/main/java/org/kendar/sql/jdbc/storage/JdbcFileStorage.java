package org.kendar.sql.jdbc.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JdbcFileStorage extends BaseFileStorage<JdbcRequest, JdbcResponse> implements JdbcStorage {


    private static final Logger log = LoggerFactory.getLogger(JdbcFileStorage.class);
    private static final Object lockObject = new Object();
    private static final List<String> toAvoid = List.of("SET", "CREATE", "DELETE", "DROP");
    private final ConcurrentHashMap<Long, StorageItem<JdbcRequest, JdbcResponse>> inMemoryDb = new ConcurrentHashMap<>();
    private boolean initialized = false;
    private List<CompactLine> index = new ArrayList<>();


    public JdbcFileStorage(String targetDir) {
        super(targetDir);
    }

    public JdbcFileStorage(Path targetDir) {
        super(targetDir);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JdbcRequest, JdbcResponse>>() {
        };
    }

    @Override
    public void write(int connectionId, String query, int intResult, List<BindingParameter> parameterValues, long durationMs, String type) {
        var item = new StorageItem(
                connectionId,
                mapper.serializePretty(new JdbcRequest(query, parameterValues)),
                mapper.serializePretty(new JdbcResponse(intResult)),
                durationMs, type, "JDBC");
        write(item);
    }

    @Override
    public void write(int connectionId, String query, SelectResult selectResult, List<BindingParameter> parameterValues, long durationMs, String type) {
        var item = new StorageItem(
                connectionId,
                new JdbcRequest(query, parameterValues),
                new JdbcResponse(selectResult),
                durationMs, type, "JDBC");
        write(item);
    }

    private void initializeContent() {
        if (!initialized) {
            for (var item : readAllItems()) {
                inMemoryDb.put(item.getIndex(), item);
            }

            index = retrieveIndexFile();
            initialized = true;
        }
    }

    @Override
    public StorageItem read(String query, List<BindingParameter> parameterValues, String type) {
        synchronized (lockObject) {
            initializeContent();
            var item = inMemoryDb.values().stream()
                    .filter(a -> {
                        var req = (JdbcRequest) a.getInput();
                        return req.getQuery().equalsIgnoreCase(query) &&
                                type.equalsIgnoreCase(a.getType()) &&
                                parameterValues.size() == req.getParameterValues().size() &&
                                a.getCaller().equalsIgnoreCase("JDBC");
                    }).findFirst();

            var idx = index.stream()
                    .filter(a -> type.equalsIgnoreCase(a.getType()) &&
                            a.getCaller().equalsIgnoreCase("JDBC") &&
                            a.getTags().get("query") != null && a.getTags().get("query").equalsIgnoreCase(query)).findFirst();
            CompactLine cl = null;
            if (idx.isPresent()) {
                cl = idx.get();
            }
            var shouldNotSave = shouldNotSave(cl, null, null, null);

            if (item.isPresent() && !shouldNotSave) {

                log.debug("[SERVER][REPFULL]  {}:{}", item.get().getIndex(), item.get().getType());
                inMemoryDb.remove(item.get().getIndex());
                idx.ifPresent(compactLine -> index.remove(compactLine));
                return beforeSendingReadResult(item.get());
            }

            if (idx.isPresent()) {
                log.debug("[SERVER][REPSHRT] {}:{}", idx.get().getIndex(), idx.get().getType());
                index.remove(idx.get());
                var si = new StorageItem<JdbcRequest, JdbcResponse>();
                JdbcResponse resp = new JdbcResponse();
                if (idx.get().getTags().get("isIntResult").equalsIgnoreCase("true")) {
                    resp.setIntResult(Integer.parseInt(idx.get().getTags().get("resultsCount")));
                    SelectResult resultset = new SelectResult();
                    resultset.setIntResult(true);
                    resultset.setCount(resp.getIntResult());
                    resp.setSelectResult(resultset);
                }
                si.setOutput(resp);
                JdbcRequest req = new JdbcRequest();
                req.setQuery(idx.get().getTags().get("query"));
                si.setInput(req);
                return beforeSendingReadResult(si);
            }

            return null;
        }
    }

    protected StorageItem beforeSendingReadResult(StorageItem<JdbcRequest, JdbcResponse> si) {
        return si;
    }

    protected Map<String, String> buildTag(StorageItem<JdbcRequest, JdbcResponse> item) {
        var data = new HashMap<String, String>();
        data.put("query", null);
        data.put("isIntResult", "true");
        data.put("parametersCount", "0");
        data.put("resultsCount", "0");
        if (item.getInput() != null) {
            if (item.getInput().getQuery() != null) {
                data.put("query", item.getInput().getQuery());
            }
            if (item.getInput().getParameterValues() != null) {
                data.put("parametersCount", item.getInput().getParameterValues().size() + "");

            }
        }
        if (item.getOutput() != null) {
            data.put("isIntResult", Boolean.toString(item.getOutput().getSelectResult().isIntResult()));
            if (item.getOutput().getSelectResult().isIntResult()) {
                data.put("resultsCount", item.getOutput().getIntResult() + "");
            } else {
                data.put("resultsCount", item.getOutput().getSelectResult().getCount() + "");
            }
        }
        return data;
    }

    @Override
    protected boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JdbcRequest, JdbcResponse> item,
                                    List<StorageItem<JdbcRequest, JdbcResponse>> loadedData) {
        if (useFullData) return false;
        if (cl == null) return false;
        if (cl.getTags() == null || cl.getTags().get("query") == null) {
            return false;
        }
        for (var i = 0; i < toAvoid.size(); i++) {
            var toAvoidSingle = toAvoid.get(i).toUpperCase();
            if (cl.getTags().get("query").toUpperCase().trim().startsWith(toAvoidSingle)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StorageItem<JdbcRequest, JdbcResponse> read(JdbcRequest toRead, String type) {
        throw new RuntimeException("PUSH NOT IMPLEMENTED EXCEPTION");
    }

    @Override
    public List<StorageItem<JdbcRequest, JdbcResponse>> readResponses(long afterIndex) {
        throw new RuntimeException("PUSH NOT IMPLEMENTED EXCEPTION");
    }
}
