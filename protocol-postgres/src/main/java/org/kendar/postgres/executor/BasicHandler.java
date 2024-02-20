package org.kendar.postgres.executor;

import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public interface BasicHandler {
    boolean isMatching(String query);

    Iterator<ProtoStep> execute(ProtoContext protoContext, Parse parse, Binding binding, int maxRecords, boolean describable);
}
