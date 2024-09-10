package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.PostgresProtocol;
import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.postgres.executor.converters.PostgresDataConverter;
import org.kendar.postgres.messages.BindComplete;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.sql.jdbc.BindingParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Iterator;

import static org.kendar.postgres.executor.converters.PostgresDataConverter.bytesToJava;

public class Bind extends PostgresState {
    private static final Logger log = LoggerFactory.getLogger(Bind.class);

    public Bind(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'B';
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer message, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        var portal = message.getString();
        var statementName = message.getString();
        var formatCodesCount = message.getShort();
        var formatCodes = new ArrayList<Short>();
        short parameterValuesCount;
        for (var i = 0; i < formatCodesCount; i++) {
            formatCodes.add(message.getShort());
        }
        parameterValuesCount = message.getShort();
        var parse = (Parse) postgresContext.getValue("STATEMENT_" + statementName);

        var parameterValues = new ArrayList<BindingParameter>();

        var converter = PostgresProtocol.getDataTypesConverter();
        for (var i = 0; i < parameterValuesCount; i++) {
            var parameterLength = message.getInt();
            var oid = parse.getOids().get(i);

            var jdbcType = converter.getDttFromNative(oid);
            var clazz = converter.getFromName(jdbcType.getClassName());
            if (parameterLength >= 0) {
                var bytes = message.getBytes(parameterLength);
                if (formatCodes.get(i) == 0) {
                    var bb = protoContext.buildBuffer();
                    bb.write(bytes);
                    bb.setPosition(0);
                    var converted = PostgresDataConverter.convert(bb.getUtf8String(), clazz);
                    parameterValues.add(new BindingParameter(converted.toString(), false, false, jdbcType.extractJdbcType()));
                } else {
                    parameterValues.add(convert(bytes, clazz.getSimpleName(), protoContext, false, jdbcType.extractJdbcType()));
                }
            } else {
                parameterValues.add(new BindingParameter(null, formatCodes.get(i) != 0, true, jdbcType.extractJdbcType()));
            }
        }

        var count = message.getShort();


        log.debug("[SERVER][STMTBIND]\tSTATEMENT_{} Count:{} Query: {}", statementName, count, parse.getQuery());
        var bindMessage = new Binding("STATEMENT_" + statementName, portal, formatCodes, parameterValues);
        parse.put("PORTAL_" + portal, bindMessage);
        postgresContext.setValue("PORTAL_" + portal, bindMessage);


        postgresContext.addSync(iteratorOfList(new BindComplete()));
        return iteratorOfEmpty();
    }

    private BindingParameter convert(byte[] bytes, String simpleClassName, NetworkProtoContext protoContext, boolean isOutput, JDBCType dataType) {
        Object value = bytesToJava(bytes, simpleClassName, protoContext, isOutput);
        return new BindingParameter(value.toString(), value == bytes, isOutput, dataType);
    }


}
