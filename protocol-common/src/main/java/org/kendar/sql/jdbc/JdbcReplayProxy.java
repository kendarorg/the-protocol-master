package org.kendar.sql.jdbc;

import org.kendar.protocol.ProtoContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.sql.jdbc.storage.JdbcStorage;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.storage.StorageItem;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SqlSourceToSinkFlow")
public class JdbcReplayProxy extends JdbcProxy {

    public JdbcReplayProxy(JdbcStorage jdbcStorage) {
        super(null,null,null,null);
        setStorage(jdbcStorage);

    }



    public SelectResult executeQuery(boolean insert, String query,
                                     Object connection, int maxRecords,
                                     List<BindingParameter> parameterValues,
                                     SqlStringParser parser,
                                     ArrayList<JDBCType> concreteTypes) {

            long start = System.currentTimeMillis();
            StorageItem storageItem = storage.read(query, parameterValues, "QUERY");
            var duration = storageItem.getDurationMs();

            return ((JdbcResponse) storageItem.getOutput()).getSelectResult();
    }



    @Override
    public ProxyConnection connect() {
        return new ProxyConnection(null);
    }

    @Override
    public void initialize() {

    }


    public void executeBegin(ProtoContext protoContext) {

    }

    public void executeCommit(ProtoContext protoContext) {

    }

    public void executeRollback(ProtoContext protoContext) {

    }

    public void setIsolation(ProtoContext protoContext, int transactionIsolation) {

    }
}
