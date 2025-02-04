package org.kendar.mysql.executor;

import org.kendar.mysql.MySqlProtocolSettings;
import org.kendar.mysql.constants.ErrorCode;
import org.kendar.mysql.constants.Language;
import org.kendar.mysql.constants.StatusFlag;
import org.kendar.mysql.messages.Error;
import org.kendar.mysql.messages.*;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.ProxyMetadata;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.parser.SqlParseResult;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.SqlStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class MySQLExecutor {
    public static final String TRANSACTION_ISOLATION_LEVEL_ID = "TRANSACTION ISOLATION LEVEL";
    protected static final List<BasicHandler> fakeQueries;
    private static final Logger log = LoggerFactory.getLogger(MySQLExecutor.class);

    static {
        fakeQueries = new ArrayList<>();
        //fakeQueries.add(new SetHandler());//"SET extra_float_digits".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select oid, typbasetype from pg_type where typname = 'lo'".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select nspname from pg_namespace".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select n.nspname, c.relname, a.attname, a.atttypid".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select current_schema()".toLowerCase(Locale.ROOT));


        //fakeQueries.add("SET application_name".toLowerCase(Locale.ROOT));
        //fakeQueries.add("set client_encoding".toLowerCase(Locale.ROOT));
        //fakeQueries.add("set statement_timeout".toLowerCase(Locale.ROOT));

    }

    private SqlStringParser parser;

    private static Iterator<ProtoStep> executeCommit(String parse, ProtoContext protoContext) {
        var mysqlContext = (MySQLProtoContext) protoContext;
        ((JdbcProxy) mysqlContext.getProxy()).executeCommit(protoContext);
        protoContext.setValue("TRANSACTION", false);
        var ok = new OkPacket();
        var force3Bytes = ((MySqlProtocolSettings) protoContext.getDescriptor().getSettings()).isForce3BytesOkPacketInfo();
        ok.setForce3BytesOkPacketInfo(force3Bytes);
        ok.setStatusFlags(StatusFlag.SERVER_STATUS_AUTOCOMMIT.getCode());
        return ProtoState.iteratorOfList(ok);
    }

    private static Iterator<ProtoStep> executeRollback(String parse, ProtoContext protoContext) {
        var mysqlContext = (MySQLProtoContext) protoContext;
        ((JdbcProxy) mysqlContext.getProxy()).executeRollback(protoContext);
        protoContext.setValue("TRANSACTION", false);
        var ok = new OkPacket();
        var force3Bytes = ((MySqlProtocolSettings) protoContext.getDescriptor().getSettings()).isForce3BytesOkPacketInfo();
        ok.setForce3BytesOkPacketInfo(force3Bytes);
        ok.setStatusFlags(StatusFlag.SERVER_STATUS_AUTOCOMMIT.getCode());
        return ProtoState.iteratorOfList(ok);
    }

    private static Iterator<ProtoStep> executeBegin(String parse, ProtoContext protoContext) {
        var mysqlContext = (MySQLProtoContext) protoContext;
        ((JdbcProxy) mysqlContext.getProxy()).executeBegin(protoContext);
        protoContext.setValue("TRANSACTION", true);
        var ok = new OkPacket();
        var force3Bytes = ((MySqlProtocolSettings) protoContext.getDescriptor().getSettings()).isForce3BytesOkPacketInfo();
        ok.setForce3BytesOkPacketInfo(force3Bytes);
        ok.setStatusFlags(StatusFlag.SERVER_STATUS_IN_TRANS.getCode());
        return ProtoState.iteratorOfList(ok);
    }

    public Iterator<ProtoStep> executeText(MySQLProtoContext protoContext, String parse, List<BindingParameter> parameterValues, boolean text) {

        try {
            parser = (SqlStringParser) protoContext.getValue("PARSER");
            if (parse.trim().isEmpty()) {
                //return new ExecutorResult(ProtoState.iteratorOfList(new EmptyQueryResponse()));
                throw new RuntimeException("MISSING QUERY");
            }

            if (protoContext.getProxy() == null) {
                //noinspection ForLoopReplaceableByForEach
                for (var i = 0; i < fakeQueries.size(); i++) {
                    var query = fakeQueries.get(i);
                    if (query.isMatching(parse)) {
                        return query.execute(protoContext, parse);
                    }
                }
            }
            return executeRealQuery(protoContext, parse, parameterValues, text);
        } catch (RuntimeException ex) {
            return runExceptionInternal(protoContext, ex);
        }
    }

    protected Iterator<ProtoStep> runExceptionInternal(MySQLProtoContext context, Exception ex) {
        var error = new Error();
        log.error(ex.getMessage(), ex);
        error.setCapabilities(context.getClientCapabilities());
        error.setErrorCode(ErrorCode.ER_UNKNOWN_COM_ERROR.getValue());
        error.setErrorMessage(ex.getMessage());
        error.setSqlState("08S01");
        return ProtoState.iteratorOfList(error);
    }

    protected boolean shouldHandleAsSingleQuery(List<SqlParseResult> parsed) {
        return parser.isUnknown(parsed) || parser.isMixed(parsed) || parsed.size() == 1;
    }

    private Iterator<ProtoStep> executeRealQuery(MySQLProtoContext protoContext, String parse, List<BindingParameter> parameterValues, boolean text) {
        if (!parse.trim().endsWith(";")) {
            parse = parse + ";";
        }
        var parsed = parser.getTypes(parse);
        try {
            //CapabilityFlag.isFlagSet(protoContext.getClientCapabilities(), CapabilityFlag.CLIENT_MULTI_STATEMENTS)
            if (!shouldHandleAsSingleQuery(parsed) || (
                    parsed.size() == 2 && parsed.get(0).getType() == SqlStringType.INSERT
                            && parsed.get(1).getType() == SqlStringType.SELECT
            )) {
                return handleWithinTransaction(parsed, protoContext, parse, parameterValues, text);
            } else {
                var sqlParseResult = new SqlParseResult(parse, parsed.get(0).getType());
                return handleSingleQuery(parsed.get(0).getValue(), sqlParseResult, protoContext, parse, parameterValues, text);
            }
        } catch (SQLException e) {
            log.error("[SERVER] Error %s", e);
            return runExceptionInternal(protoContext, e);
        } catch (RuntimeException e) {
            var uuid = UUID.randomUUID().toString();
            log.error("[SERVER] Runtime Error {} {}", uuid, e.getMessage());
            log.error("[SERVER] Runtime Error {}", uuid, e);

            return runExceptionInternal(protoContext, e);
        }
    }

    private Iterator<ProtoStep> handleSingleQuery(String cleanedUpString, SqlParseResult parsed, MySQLProtoContext
            protoContext, String parse, List<BindingParameter> parameterValues, boolean text) throws SQLException {
        var pvup = parsed.getValue().toUpperCase();
        if (pvup.startsWith("SET AUTOCOMMIT=0")) {
            return executeBegin(parse, protoContext);
        } else if (pvup.startsWith("SET AUTOCOMMIT=1")) {
            return executeCommit(parse, protoContext);
        } else if (pvup.startsWith("ROLLBACK")) {
            return executeRollback(parse, protoContext);
        }
        return switch (parsed.getType()) {
            case UPDATE -> executeQuery(999999, parsed, protoContext, parse, "UPDATE", parameterValues, text);
            case INSERT -> executeQuery(999999, parsed, protoContext, parse, "INSERT 0", parameterValues, text);
            case SELECT -> executeQuery(999999, parsed, protoContext, parse, "SELECT", parameterValues, text);
            case CALL -> executeQuery(999999, parsed, protoContext, parse, "CALL", parameterValues, text);
            default -> {
                if (cleanedUpString.toUpperCase().startsWith("BEGIN")) {
                    yield executeBegin(parse, protoContext);
                } else if (cleanedUpString.toUpperCase().startsWith("COMMIT")) {
                    yield executeCommit(parse, protoContext);
                } else if (cleanedUpString.toUpperCase().startsWith("ROLLBACK")) {
                    yield executeRollback(parse, protoContext);
                } else if (cleanedUpString.toUpperCase().startsWith("USE")) {
                    yield executeQuery(999999, parsed, protoContext, parse, "INSERT 0", parameterValues, text);
                }
                throw new SQLException("UNSUPPORTED QUERY " + parsed.getValue());
            }
        };
    }

    private Iterator<ProtoStep> changeTransactionIsolation(ProtoContext protoContext, String value) {
        var mysqlContext = (MySQLProtoContext) protoContext;
        var pattern = Pattern.compile("(.+)" + TRANSACTION_ISOLATION_LEVEL_ID + "([\\s]*)([\\sa-zA-Z\\-_0-9]+)");
        var matcher = pattern.matcher(value);
        var result = matcher.matches();
        if (result) {
            var transactionType = matcher.group(3).toUpperCase();
            switch (transactionType) {
                case ("REPEATABLE READ"):
                    ((JdbcProxy) mysqlContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_REPEATABLE_READ);
                    break;
                case ("READ UNCOMMITTED"):
                    ((JdbcProxy) mysqlContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_READ_UNCOMMITTED);
                    break;
                case ("READ COMMITTED"):
                    ((JdbcProxy) mysqlContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_READ_COMMITTED);
                    break;
                case ("SERIALIZABLE"):
                    ((JdbcProxy) mysqlContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_SERIALIZABLE);
                    break;
                default:
                    throw new RuntimeException("Unsupported isolation " + transactionType);


            }
        }

        return ProtoState.iteratorOfList(new EOFPacket());
    }

    private Iterator<ProtoStep> executeQuery(int maxRecords, SqlParseResult parsed, MySQLProtoContext protoContext, String parse, String operation, List<BindingParameter> parameterValues, boolean text) throws SQLException {
        var connection = protoContext.getValue("CONNECTION");
        if (maxRecords == 0) {
            maxRecords = Integer.MAX_VALUE;
        }
        var matcher = parsed.getValue();
        SelectResult resultSet;
        if (!matcher.equalsIgnoreCase(parsed.getValue())) {
            resultSet = ((JdbcProxy) protoContext.getProxy()).executeQuery(protoContext.getContextId(),
                    parsed.getType() == SqlStringType.INSERT,
                    matcher, connection, maxRecords, parameterValues, parser,
                    new ArrayList<>(), protoContext);

        } else {
            resultSet = ((JdbcProxy) protoContext.getProxy()).executeQuery(protoContext.getContextId(),
                    parsed.getType() == SqlStringType.INSERT,
                    parsed.getValue(), connection, maxRecords,
                    parameterValues, parser, new ArrayList<>(), protoContext);
        }
        var result = new ArrayList<ReturnMessage>();

        if (parsed.getType() == SqlStringType.INSERT) {
            if (resultSet.getRecords().size() == 1 && resultSet.getMetadata().size() == 1 &&
                    resultSet.getMetadata().get(0).getColumnName().equalsIgnoreCase("GENERATED_KEY")) {
                resultSet.setIntResult(true);
                resultSet.setLastInsertedId(Long.parseLong(resultSet.getRecords().get(0).get(0)));
                resultSet.getMetadata().clear();
                resultSet.getRecords().clear();
            }
        }

        if (!resultSet.isIntResult()) {
            var packetNumber = 0;

            result.add(new ColumnsCount(resultSet.getMetadata()).
                    withPacketNumber(++packetNumber));
            for (var field : resultSet.getMetadata()) {
                result.add(new ColumnDefinition(field, Language.UTF8_GENERAL_CI, false).
                        withPacketNumber(++packetNumber));
            }
            //}
            //TODO Check for the
            // if CLIENT_DEPRECATE_EOF is on, OK_Packet is sent instead of an actual EOF_Packet packet.
            //https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response.html

            result.add(new EOFPacket().
                    withStatusFlags(0x022).
                    withPacketNumber(++packetNumber));

            for (var byteRow : resultSet.getRecords()) {
                protoContext.updateLastAccess();
                if (text) {
                    //Text resultset
                    result.add(new DataRow(byteRow, resultSet.getMetadata()).
                            withPacketNumber(++packetNumber));
                } else {
                    //Binary resultset
                    result.add(new BinaryDataRow(byteRow, resultSet.getMetadata()).
                            withPacketNumber(++packetNumber));
                }
            }
            result.add(new EOFPacket().
                    withPacketNumber(++packetNumber));
        } else {
            var okPacket = new OkPacket();
            var force3Bytes = ((MySqlProtocolSettings) protoContext.getDescriptor().getSettings()).isForce3BytesOkPacketInfo();
            okPacket.setForce3BytesOkPacketInfo(force3Bytes);
            okPacket.setStatusFlags(0x022); //TODO
            okPacket.setWarnings(0);
            okPacket.setPacketNumber(1);
            okPacket.setLastInsertId(resultSet.getLastInsertedId());
            okPacket.setAffectedRows(resultSet.getCount());
            result.add(okPacket);
        }


        return ProtoState.iteratorOfList(result.toArray(new ReturnMessage[0]));
    }

    private Iterator<ProtoStep> handleWithinTransaction(List<SqlParseResult> parseds, MySQLProtoContext protoContext, String parse, List<BindingParameter> parameterValues, boolean text) throws SQLException {
        ((JdbcProxy) protoContext.getProxy()).executeBegin(protoContext);
        Iterator<ProtoStep> lastOne = null;
        for (var parsed : parseds) {
            var sqlParseResult = new SqlParseResult(parsed.getValue(), parsed.getType());
            lastOne = handleSingleQuery(parsed.getValue(), sqlParseResult, protoContext, parse, parameterValues, text);
        }
        ((JdbcProxy) protoContext.getProxy()).executeCommit(protoContext);
        return lastOne;
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public Iterator<ProtoStep> prepareStatement(MySQLProtoContext protoContext, String query) {
        parser = (SqlStringParser) protoContext.getValue("PARSER");
        var result = new ArrayList<ReturnMessage>();
        var connection = protoContext.getValue("CONNECTION");
        var c = ((Connection) ((ProxyConnection) connection).getConnection());
        try {
            long start = System.currentTimeMillis();
            var fields = new ArrayList<ProxyMetadata>();
            var ps = c.prepareStatement(query);
            var parameterMetaData = ps.getParameterMetaData();
            for (var i = 0; i < parameterMetaData.getParameterCount(); i++) {
                var isByte = JdbcProxy.isByteOut(parameterMetaData.getParameterClassName(i + 1));
                fields.add(new ProxyMetadata(
                        "?",
                        isByte,
                        parameterMetaData.getParameterType(i + 1),
                        parameterMetaData.getScale(i + 1)));
            }


            var resultSetMetaData = ps.getMetaData();
            if (resultSetMetaData != null) {
                for (var i = 0; i < resultSetMetaData.getColumnCount(); i++) {
                    var isByte = JdbcProxy.isByteOut(resultSetMetaData.getColumnClassName(i + 1));
                    var name = (resultSetMetaData.getColumnLabel(i + 1) == null || resultSetMetaData.getColumnLabel(i + 1).isEmpty()) ?
                            resultSetMetaData.getColumnName(i + 1) : resultSetMetaData.getColumnLabel(i + 1);
                    fields.add(new ProxyMetadata(
                            name,
                            isByte,
                            resultSetMetaData.getColumnType(i + 1),
                            resultSetMetaData.getPrecision(i + 1)));
                }
            }
            int currentStatementId = protoContext.getDescriptor().getCounter("STATEMENT_ID");
            var packetNumber = 0;
            result.add(new ComStmtPrepareOk(fields, currentStatementId)
                    .withPacketNumber(++packetNumber));
            var someColumn = false;
            for (var field : fields) {
                if (field.getColumnName().equalsIgnoreCase("?")) {
                    result.add(new ColumnDefinition(field, Language.UTF8_GENERAL_CI, true).
                            withPacketNumber(++packetNumber));
                    someColumn = true;
                }
            }

            if (someColumn && resultSetMetaData != null) {
                result.add(new EOFPacket().
                        withStatusFlags(0x022).
                        withPacketNumber(++packetNumber));
            }

            //Send the parameters AND the resultset metadata
            if (resultSetMetaData != null) {

                for (var field : fields) {
                    if (!field.getColumnName().equalsIgnoreCase("?")) {
                        result.add(new ColumnDefinition(field, Language.UTF8_GENERAL_CI, true).
                                withPacketNumber(++packetNumber));
                    }
                }
            }


            result.add(new EOFPacket().
                    withStatusFlags(0x022).
                    withPacketNumber(++packetNumber));

            protoContext.setValue("STATEMENT_" + currentStatementId, query);
            protoContext.setValue("STATEMENT_FIELDS_" + currentStatementId, fields);
            ps.close();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return ProtoState.iteratorOfList(result.toArray(new ReturnMessage[0]));
    }
}
