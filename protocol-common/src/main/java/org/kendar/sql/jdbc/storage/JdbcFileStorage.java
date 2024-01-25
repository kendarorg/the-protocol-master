package org.kendar.sql.jdbc.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.StorageItem;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class JdbcFileStorage extends BaseFileStorage<JdbcRequest, JdbcResponse> implements JdbcStorage {


    private ConcurrentHashMap<Long, StorageItem<JdbcRequest, JdbcResponse>> inMemoryDb = new ConcurrentHashMap<>();
    private boolean initialized = false;

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
    public void write(String query, int intResult, List<BindingParameter> parameterValues, long durationMs, String type) {
        var item = new StorageItem(
                mapper.serialize(new JdbcRequest(query, parameterValues)),
                mapper.serialize(new JdbcResponse(intResult)),
                durationMs, type, "JDBC");
        write(item);
    }

    @Override
    public void write(String query, SelectResult selectResult, List<BindingParameter> parameterValues, long durationMs, String type) {
        var item = new StorageItem(
                new JdbcRequest(query, parameterValues),
                new JdbcResponse(selectResult),
                durationMs, type, "JDBC");
        write(item);
    }

    @Override
    public StorageItem read(String query, List<BindingParameter> parameterValues, String type) {
        if (!initialized) {
            for (var item : readAllItems()) {
                inMemoryDb.put(item.getIndex(), item);
            }
            initialized = true;
        }
        var item = inMemoryDb.values().stream()
                .filter(a -> {
                    var req = (JdbcRequest) a.getInput();
                    return req.getQuery().equalsIgnoreCase(query) &&
                            type.equalsIgnoreCase(a.getType()) &&
                            parameterValues.size() == req.getParameterValues().size() &&
                            a.getCaller().equalsIgnoreCase("JDBC");
                }).findFirst();
        if (item.isPresent()) {
            inMemoryDb.remove(item.get().getIndex());
            return item.get();
        }

        return null;
    }


}
