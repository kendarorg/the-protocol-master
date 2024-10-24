package org.kendar.sql.jdbc.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.storage.BaseStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.StorageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcFileStorage extends BaseStorage<JdbcRequest, JdbcResponse> implements JdbcStorage {


    private static final List<String> toAvoid = List.of("SET", "CREATE", "DELETE", "DROP");

    public JdbcFileStorage(StorageRepository<JdbcRequest, JdbcResponse> repository) {
        super(repository);
    }

    @Override
    public String getCaller() {
        return "JDBC";
    }

    @Override
    public StorageItem beforeSendingReadResult(StorageItem<JdbcRequest, JdbcResponse> si, CompactLine idx) {
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
        return si;
    }

    @Override
    public TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JdbcRequest, JdbcResponse>>() {
        };
    }

    @Override
    public void write(int connectionId, String query, int intResult, List<BindingParameter> parameterValues, long durationMs, String type) {
        var item = new StorageItem(
                connectionId,
                mapper.serializePretty(new JdbcRequest(query, parameterValues)),
                mapper.serializePretty(new JdbcResponse(intResult)),
                durationMs, type, getCaller());
        write(item);
    }

    @Override
    public void write(int connectionId, String query, SelectResult selectResult, List<BindingParameter> parameterValues, long durationMs, String type) {
        var item = new StorageItem(
                connectionId,
                new JdbcRequest(query, parameterValues),
                new JdbcResponse(selectResult),
                durationMs, type, getCaller());
        write(item);
    }


    @Override
    public StorageItem read(String query, List<BindingParameter> parameterValues, String type) {

        var siQuery = new CallItemsQuery();
        siQuery.setCaller(getCaller());
        siQuery.setType(type);
        siQuery.addTag("parametersCount", parameterValues.size());
        siQuery.addTag("query", query);
        return read(siQuery);
    }

    @Override
    public Map<String, String> buildTag(StorageItem<JdbcRequest, JdbcResponse> item) {
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
    public boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JdbcRequest, JdbcResponse> item,
                                 List<StorageItem<JdbcRequest, JdbcResponse>> loadedData) {
        if (useFullData()) return false;
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
