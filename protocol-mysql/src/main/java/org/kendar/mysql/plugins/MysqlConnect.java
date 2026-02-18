package org.kendar.mysql.plugins;

import org.kendar.events.TpmEvent;
import org.kendar.mysql.executor.MySQLProtoContext;

public class MysqlConnect implements TpmEvent {
    private final MySQLProtoContext protoContext;

    public MysqlConnect(MySQLProtoContext protoContext) {
        this.protoContext = protoContext;
    }

    public MySQLProtoContext getProtoContext() {
        return protoContext;
    }
}
