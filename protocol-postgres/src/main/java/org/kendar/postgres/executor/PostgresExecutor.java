package org.kendar.postgres.executor;

import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Field;
import org.kendar.postgres.dtos.Parse;
import org.kendar.postgres.executor.converters.PostgresCallConverter;
import org.kendar.postgres.fsm.PostgresProtoContext;
import org.kendar.postgres.messages.*;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.parser.SqlParseResult;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.SqlStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class PostgresExecutor {

    public static final String TRANSACTION_ISOLATION_LEVEL_ID = "TRANSACTION ISOLATION LEVEL";
    protected static final List<BasicHandler> fakeQueries;
    private static final Logger log = LoggerFactory.getLogger(PostgresExecutor.class);

    static {
        fakeQueries = new ArrayList<>();
        fakeQueries.add(new SetHandler());//"SET extra_float_digits".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select oid, typbasetype from pg_type where typname = 'lo'".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select nspname from pg_namespace".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select n.nspname, c.relname, a.attname, a.atttypid".toLowerCase(Locale.ROOT));
//        fakeQueries.add("select current_schema()".toLowerCase(Locale.ROOT));


        //fakeQueries.add("SET application_name".toLowerCase(Locale.ROOT));
        //fakeQueries.add("set client_encoding".toLowerCase(Locale.ROOT));
        //fakeQueries.add("set statement_timeout".toLowerCase(Locale.ROOT));

    }

    private SqlStringParser parser;

    private static ExecutorResult executeCommit(Parse parse, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        ((JdbcProxy) protoContext.getProxy()).executeCommit(protoContext);
        var res = new ArrayList<ReturnMessage>();
        protoContext.setValue("TRANSACTION", false);

        if (postgresContext.getValue("STATEMENT_" + parse.getStatementName() + "_PARSED") == null) {
            res.add(new ParseComplete());
        }
        res.add(new BindComplete());
        res.add(new CommandComplete("COMMIT"));
        return new ExecutorResult(ProtoState.iteratorOfList(res.toArray(new ReturnMessage[0]))).runNow();
    }

    private static ExecutorResult executeRollback(Parse parse, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        ((JdbcProxy) protoContext.getProxy()).executeRollback(protoContext);
        var res = new ArrayList<ReturnMessage>();
        protoContext.setValue("TRANSACTION", false);


        res.add(new ParseComplete());
        res.add(new BindComplete());
        res.add(new CommandComplete("ROLLBACK"));
        return new ExecutorResult(ProtoState.iteratorOfList(res.toArray(new ReturnMessage[0]))).runNow();
    }

    private static ExecutorResult executeBegin(Parse parse, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        ((JdbcProxy) protoContext.getProxy()).executeBegin(protoContext);
        var res = new ArrayList<ReturnMessage>();
        protoContext.setValue("TRANSACTION", true);

        //if(postgresContext.getValue("STATEMENT_"+parse.getStatementName()+"_PARSED")==null)
        {
            res.add(new ParseComplete());
        }
        res.add(new BindComplete());
        res.add(new CommandComplete("BEGIN"));
        return new ExecutorResult(ProtoState.iteratorOfList(res.toArray(new ReturnMessage[0]))).runNow();
    }

    private static List<Field> identifyFields(SelectResult resultSet) throws SQLException {
        var resultSetMetaData = resultSet.getMetadata();
        var fields = new ArrayList<Field>();
        //noinspection ForLoopReplaceableByForEach
        for (var i = 0; i < resultSetMetaData.size(); i++) {
            var md = resultSetMetaData.get(i);
            fields.add(new Field(
                    md.getColumnName(), md.isByteData()));
        }
        return fields;
    }

    public ExecutorResult executePortal(PostgresProtoContext protoContext, Parse parse, Binding binding,
                                        int maxRecords, boolean describable, boolean possiblyMultiple) {

        try {
            parser = (SqlStringParser) protoContext.getValue("PARSER");
            if (parse.getQuery().trim().isEmpty()) {
                return new ExecutorResult(ProtoState.iteratorOfList(new EmptyQueryResponse()));
            }

            if (protoContext.getProxy() == null) {
                //noinspection ForLoopReplaceableByForEach
                for (var i = 0; i < fakeQueries.size(); i++) {
                    var query = fakeQueries.get(i);
                    if (query.isMatching(parse.getQuery())) {
                        return new ExecutorResult(query.execute(protoContext, parse, binding, maxRecords, describable));
                    }
                }
            }
            return executeRealQuery(protoContext, parse, binding, maxRecords, describable, possiblyMultiple);
        } catch (RuntimeException ex) {
            log.error(ex.getMessage(), ex);
            return new ExecutorResult(ProtoState.iteratorOfList(new ErrorResponse(ex.getMessage())));
        }
    }

    protected boolean shouldHandleAsSingleQuery(List<SqlParseResult> parsed) {
        return parser.isUnknown(parsed) || parser.isMixed(parsed) || parsed.size() == 1;
    }

    private ExecutorResult executeRealQuery(PostgresProtoContext protoContext, Parse parse,
                                            Binding binding, int maxRecords, boolean describable,
                                            boolean possiblyMultiple) {
        var parsed = parser.getTypes(parse.getQuery());
        var returning = new ArrayList<ReturnMessage>();
        try {
            if (!shouldHandleAsSingleQuery(parsed) && possiblyMultiple) {
                return handleWithinTransaction(parsed, protoContext, parse, binding, maxRecords, describable);
                //TODO transaction
            } else {
                var sqlParseResult = new SqlParseResult(parse.getQuery(), parsed.get(0).getType());
                return handleSingleQuery(sqlParseResult, protoContext, parse, binding, maxRecords, describable);
            }
        } catch (SQLException e) {
            log.error("[SERVER] Error {}", e.getMessage());
            return new ExecutorResult(ProtoState.iteratorOfList(new ErrorResponse(e.getMessage()))).runNow();
        } catch (RuntimeException e) {
            var uuid = UUID.randomUUID().toString();
            log.error("[SERVER] Runtime Error {} {}", uuid, e.getMessage());
            log.error("[SERVER] Runtime Error {}", uuid, e);

            return new ExecutorResult(ProtoState.iteratorOfList(new ErrorResponse(e.getMessage()))).runNow();
        }
    }

    private ExecutorResult handleSingleQuery(SqlParseResult parsed,
                                             NetworkProtoContext protoContext, Parse parse, Binding binding,
                                             int maxRecords, boolean describable) throws SQLException {
        switch (parsed.getType()) {
            case UPDATE:
                if (parsed.getValue().replaceAll("\\s", " ").
                        toUpperCase().contains(TRANSACTION_ISOLATION_LEVEL_ID)) {
                    return changeTransactionIsolation(protoContext, parsed.getValue().toUpperCase());
                } else {
                    return executeQuery(999999, parsed, protoContext,
                            binding, parse.getConcreteTypes(), "UPDATE");
                }
            case INSERT:
                return executeQuery(maxRecords == 1 ? -1 : 0, parsed, protoContext,
                        binding, parse.getConcreteTypes(), "INSERT 0");
            case SELECT:
                return executeQuery(maxRecords, parsed, protoContext,
                        binding, parse.getConcreteTypes(), "SELECT");
            case CALL:
                return executeQuery(maxRecords, parsed, protoContext,
                        binding, parse.getConcreteTypes(), "CALL");
            default:
                if (parsed.getValue().toUpperCase().startsWith("BEGIN")) {
                    return executeBegin(parse, protoContext);
                } else if (parsed.getValue().toUpperCase().startsWith("COMMIT")) {
                    return executeCommit(parse, protoContext);
                } else if (parsed.getValue().toUpperCase().startsWith("ROLLBACK")) {
                    return executeRollback(parse, protoContext);
                }
                throw new SQLException("UNSUPPORTED QUERY " + parsed.getValue());
        }
    }

    private ExecutorResult changeTransactionIsolation(NetworkProtoContext protoContext, String value) {

        var pattern = Pattern.compile("(.+)" + TRANSACTION_ISOLATION_LEVEL_ID + "([\\s]*)([\\sa-zA-Z\\-_0-9]+)");
        var matcher = pattern.matcher(value);
        var result = matcher.matches();
        if (result) {
            var transactionType = matcher.group(3).toUpperCase();
            switch (transactionType) {
                case ("REPEATABLE READ"):
                    ((JdbcProxy) protoContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_REPEATABLE_READ);
                    break;
                case ("READ UNCOMMITTED"):
                    ((JdbcProxy) protoContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_READ_UNCOMMITTED);
                    break;
                case ("READ COMMITTED"):
                    ((JdbcProxy) protoContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_READ_COMMITTED);
                    break;
                case ("SERIALIZABLE"):
                    ((JdbcProxy) protoContext.getProxy()).setIsolation(protoContext, Connection.TRANSACTION_SERIALIZABLE);
                    break;
                default:
                    throw new RuntimeException("Unsupported isolation " + transactionType);


            }
        }

        return new ExecutorResult(ProtoState.iteratorOfList(new CommandComplete("SELECT 0 0")));
    }


    private ExecutorResult executeQuery(int maxRecords, SqlParseResult parsed, NetworkProtoContext protoContext, Binding binding,
                                        ArrayList<JDBCType> concreteTypes, String operation) throws SQLException {
        var connection = protoContext.getValue("CONNECTION");
        var originalMaxRecords = maxRecords;

        if (parsed.getType() == SqlStringType.INSERT) {
            if (maxRecords == -1) {
                maxRecords = 0;
            } else if (maxRecords == 0) {
                maxRecords = Integer.MAX_VALUE;
            }
        } else if (maxRecords == 0) {
            maxRecords = Integer.MAX_VALUE;
        }
        var matcher = PostgresCallConverter.convertToJdbc(parsed.getValue(), binding.getParameterValues());
        SelectResult resultSet;
        if (!matcher.equalsIgnoreCase(parsed.getValue())) {
            resultSet = ((JdbcProxy) protoContext.getProxy()).executeQuery(
                    protoContext.getContextId(), parsed.getType() == SqlStringType.INSERT,
                    matcher, connection, maxRecords, binding.getParameterValues(),
                    parser, concreteTypes, protoContext);

        } else {
            resultSet = ((JdbcProxy) protoContext.getProxy()).executeQuery(
                    protoContext.getContextId(), parsed.getType() == SqlStringType.INSERT,
                    parsed.getValue(), connection, maxRecords, binding.getParameterValues(),
                    parser, concreteTypes, protoContext);
        }
        var result = new ArrayList<ReturnMessage>();
        if (originalMaxRecords == -1) {
            resultSet.setIntResult(true);
            resultSet.setCount(resultSet.getRecords().size());
            resultSet.getRecords().clear();
            resultSet.getMetadata().clear();
        }
        if (resultSet == null) {
            resultSet = new SelectResult();
            resultSet.setIntResult(true);
            resultSet.setCount(0);
        }
        if (!resultSet.isIntResult()) {

            var fields = identifyFields(resultSet);
            result.add(new RowDescription(fields));

            for (var byteRow : resultSet.getRecords()) {
                protoContext.updateLastAccess();
                result.add(new DataRow(byteRow, fields));
            }
            result.add(new CommandComplete(String.format(operation + " %d", resultSet.getRecords().size())));
        } else {
            result.add(new CommandComplete(String.format(operation + " %d", resultSet.getCount())));
        }


        return new ExecutorResult(ProtoState.iteratorOfList(result.toArray(new ReturnMessage[0])));
    }

    private ExecutorResult handleWithinTransaction(List<SqlParseResult> parseds, PostgresProtoContext protoContext, Parse parse,
                                                   Binding binding, int maxRecords, boolean describable) throws SQLException {
        ((JdbcProxy) protoContext.getProxy()).executeBegin(protoContext);
        ExecutorResult lastOne = null;
        for (var parsed : parseds) {
            var sqlParseResult = new SqlParseResult(parsed.getValue(), parsed.getType());
            lastOne = handleSingleQuery(sqlParseResult, protoContext, parse,
                    new Binding("", "", new ArrayList<>(), new ArrayList<>()), 1, false);
        }
        ((JdbcProxy) protoContext.getProxy()).executeCommit(protoContext);
        return lastOne;
    }

}
