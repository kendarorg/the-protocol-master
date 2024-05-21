package org.kendar.sql.jdbc.storage;

import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

import java.util.List;

public class NullJdbcStorage implements JdbcStorage {
    @Override
    public void initialize() {

    }

    @Override
    public Storage<JdbcRequest, JdbcResponse> withFullData() {
        return this;
    }

    @Override
    public void write(int connectionId, JdbcRequest request, JdbcResponse response, long durationMs, String type, String caller) {

    }

    @Override
    public void write(long index, int connectionId, JdbcRequest request, JdbcResponse response, long durationMs, String type, String caller) {

    }

    @Override
    public void optimize() {

    }

    @Override
    public long generateIndex() {
        return 0;
    }

    @Override
    public StorageItem<JdbcRequest, JdbcResponse> read(JdbcRequest toRead, String type) {
        return null;
    }

    @Override
    public List<StorageItem<JdbcRequest, JdbcResponse>> readResponses(long afterIndex) {
        return List.of();
    }

    @Override
    public void write(int connectionId, String query, int result, List<BindingParameter> parameterValues, long durationMs, String type) {

    }

    @Override
    public void write(int connectionId, String query, SelectResult result, List<BindingParameter> parameterValues, long durationMs, String type) {

    }

    @Override
    public StorageItem read(String query, List<BindingParameter> parameterValues, String type) {
        return null;
    }
}
