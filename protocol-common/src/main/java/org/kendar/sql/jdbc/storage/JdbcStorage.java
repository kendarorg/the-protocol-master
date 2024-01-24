package org.kendar.sql.jdbc.storage;

import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.storage.StorageItem;
import org.kendar.storage.StorageRoot;

import java.util.List;

public interface JdbcStorage extends StorageRoot<JdbcRequest, JdbcResponse> {
    void initialize();

    void write(String query, int result, List<BindingParameter> parameterValues, long durationMs, String type);

    void write(String query, SelectResult result, List<BindingParameter> parameterValues, long durationMs, String type);

    StorageItem<JdbcRequest, JdbcResponse> read(String query, List<BindingParameter> parameterValues, String type);
}
