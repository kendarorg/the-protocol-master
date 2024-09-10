package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.postgres.messages.ParseComplete;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.sql.parser.SqlStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Iterator;

import static org.kendar.sql.jdbc.JdbcProxy.parseParameters;

public class Parse extends PostgresState {
    private static final Logger log = LoggerFactory.getLogger(Parse.class);

    public Parse(Class<?>... messages) {
        super(messages);
    }

    private static void inferMetadataIfPossible(ProtoContext protoContext, String query, short paramsCount, ArrayList<JDBCType> concreteTypes, ArrayList<Integer> oids) {
        var c = ((Connection) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
        try {
            var parser = (SqlStringParser) protoContext.getValue("PARSER");
            var pst = c.prepareStatement(parseParameters(query, parser));
            var md = pst.getParameterMetaData();
            for (var i = 0; i < paramsCount; i++) {
                if (concreteTypes.get(i) == null) {
                    concreteTypes.set(i, JDBCType.valueOf(md.getParameterType(i + 1)));
                    var dtt = PostgresProtocol.getDataTypesConverter().getDttFromJdbc(md.getParameterType(i + 1));
                    oids.set(i, dtt.getDbSpecificId());
                }
            }
        } catch (Exception ex) {
            log.trace("Ignorable", ex);
        }
    }

    @Override
    protected byte getMessageId() {
        return 'P';
    }

    @Override
    public Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        var statementName = inputBuffer.getString();
        var query = inputBuffer.getUtf8String();
        log.debug("[SERVER][PARSE] Query={}", query);
        var paramsCount = inputBuffer.getShort();
        var oids = new ArrayList<Integer>();
        var concreteTypes = new ArrayList<JDBCType>();

        var hasZero = false;
        for (var i = 0; i < paramsCount; i++) {
            var oid = inputBuffer.getInt();
            oids.add(oid);
            if (oid == 0) hasZero = true;
            var jdbcType = PostgresProtocol.getDataTypesConverter().fromNativeToJdbc(oid);
            if (jdbcType.isPresent()) {
                concreteTypes.add(jdbcType.get());
            } else {
                concreteTypes.add(null);
            }
        }

        if (hasZero) {
            inferMetadataIfPossible(protoContext, query, paramsCount, concreteTypes, oids);
        }


        var oldSt = postgresContext.getValue("STATEMENT_" + statementName);
        if (oldSt != null) {
            //Cleanup
            postgresContext.setValue("STATEMENT_" + statementName, null);
            postgresContext.setValue("STATEMENT_" + statementName + "_PARSED", null);
            for (var item : ((org.kendar.postgres.dtos.Parse) oldSt).getBinds().keySet()) {
                postgresContext.setValue(item, null);
            }
        }
        postgresContext.setValue("STATEMENT_" + statementName, new org.kendar.postgres.dtos.
                Parse(statementName, query, oids, concreteTypes));
        postgresContext.setValue("STATEMENT_" + statementName + "_PARSED", true);
        postgresContext.addSync(iteratorOfList(new ParseComplete()));
        return iteratorOfEmpty();
    }
}
