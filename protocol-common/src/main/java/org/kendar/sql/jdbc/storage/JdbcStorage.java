package org.kendar.sql.jdbc.storage;

import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

import java.util.List;

public interface JdbcStorage extends Storage<JdbcRequest, JdbcResponse> {
    void initialize();

    void write(int connectionId, String query, int result, List<BindingParameter> parameterValues, long durationMs, String type);

    void write(int connectionId, String query, SelectResult result, List<BindingParameter> parameterValues, long durationMs, String type);

    StorageItem<JdbcRequest, JdbcResponse> read(String query, List<BindingParameter> parameterValues, String type);
}
