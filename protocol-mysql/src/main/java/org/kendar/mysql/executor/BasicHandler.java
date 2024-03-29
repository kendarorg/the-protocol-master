package org.kendar.mysql.executor;

import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public interface BasicHandler {
    boolean isMatching(String query);

    Iterator<ProtoStep> execute(MySQLProtoContext protoContext, String parse);
}
