package org.kendar.mssql.executor;

import org.kendar.mssql.constants.DoneStatus;
import org.kendar.mssql.messages.*;
import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.SqlStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MssqlExecutor {
    private static final Logger log = LoggerFactory.getLogger(MssqlExecutor.class);

    /**
     * Runs a query against the real server, translating the outcome to
     * TDS tokens. When inProc is set (RPC path) the statement completion
     * is a DONEINPROC, otherwise a final DONE
     */
    public List<TdsToken> executeQuery(MssqlProtoContext protoContext, String query,
                                       List<BindingParameter> parameterValues, boolean inProc) {
        var result = new ArrayList<TdsToken>();
        try {
            var cleaned = query == null ? "" : query.trim();
            if (cleaned.isEmpty()) {
                result.add(statementDone(inProc, 0, false));
                return result;
            }
            var transactionTokens = handleTransactions(protoContext, cleaned, inProc);
            if (transactionTokens != null) {
                return transactionTokens;
            }
            return executeRealQuery(protoContext, cleaned, parameterValues, inProc);
        } catch (RuntimeException ex) {
            log.error("[SERVER] Error {}", ex.getMessage());
            result.clear();
            result.add(new ErrorToken(unwrap(ex)));
            result.add(errorDone(inProc));
            return result;
        }
    }

    private String unwrap(Throwable ex) {
        var current = ex;
        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }
        var message = current.getMessage();
        if (message == null) {
            message = ex.getClass().getSimpleName();
        }
        return message;
    }

    private List<TdsToken> executeRealQuery(MssqlProtoContext protoContext, String query,
                                            List<BindingParameter> parameterValues, boolean inProc) {
        var result = new ArrayList<TdsToken>();
        var parser = (SqlStringParser) protoContext.getValue("PARSER");
        var proxy = (JdbcProxy) protoContext.getProxy();
        proxy.doConnect(protoContext);
        var connection = protoContext.getValue("CONNECTION");
        var isInsert = false;
        try {
            var parsed = parser.getTypes(query);
            isInsert = !parsed.isEmpty() && parsed.get(0).getType() == SqlStringType.INSERT;
        } catch (RuntimeException ex) {
            log.trace("Ignorable", ex);
        }
        var resultSet = proxy.executeQuery(
                protoContext.getContextId(), false, query, connection,
                Integer.MAX_VALUE, parameterValues,
                parser, new ArrayList<>(), protoContext);
        protoContext.setValue("EXECUTING_NOW", null);
        if (resultSet == null) {
            resultSet = new SelectResult();
            resultSet.setIntResult(true);
            resultSet.setCount(0);
        }
        if (!resultSet.isIntResult()) {
            result.add(new ColMetadataToken(resultSet.getMetadata()));
            for (var row : resultSet.getRecords()) {
                protoContext.updateLastAccess();
                result.add(new RowToken(row, resultSet.getMetadata()));
            }
            result.add(statementDone(inProc, resultSet.getRecords().size(), true)
                    .withCurCmd(curCmd(query)));
        } else {
            var count = Math.max(resultSet.getCount(), 0);
            result.add(statementDone(inProc, count, !isSetLike(query))
                    .withCurCmd(curCmd(query)));
        }
        return result;
    }

    private boolean isSetLike(String query) {
        var upper = query.toUpperCase(Locale.ROOT);
        return upper.startsWith("SET ") || upper.startsWith("USE ") || upper.startsWith("IF ");
    }

    /**
     * The command code of the DONE tokens: the drivers accept update
     * counts only from DML/DDL commands
     */
    private int curCmd(String query) {
        var upper = query.toUpperCase(Locale.ROOT).trim();
        if (upper.startsWith("SELECT")) return 0xC1;
        if (upper.startsWith("INSERT")) return 0xC3;
        if (upper.startsWith("DELETE")) return 0xC2;
        if (upper.startsWith("UPDATE")) return 0xC5;
        if (upper.startsWith("CREATE") || upper.startsWith("ALTER")
                || upper.startsWith("DROP") || upper.startsWith("TRUNCATE")) return 0xF0;
        return 0;
    }

    private DoneToken statementDone(boolean inProc, long count, boolean withCount) {
        var status = withCount ? DoneStatus.DONE_COUNT : DoneStatus.DONE_FINAL;
        if (inProc) {
            return new DoneInProcToken(status | DoneStatus.DONE_MORE, count);
        }
        return new DoneToken(status, count);
    }

    private TdsToken errorDone(boolean inProc) {
        if (inProc) {
            return new DoneInProcToken(DoneStatus.DONE_ERROR | DoneStatus.DONE_MORE, 0);
        }
        return new DoneToken(DoneStatus.DONE_ERROR, 0);
    }

    /**
     * mssql-jdbc drives transactions through SQL batches guarded by
     * "IF @@TRANCOUNT > 0": translate them to the jdbc proxy calls
     * instead of forwarding (they would interfere with the automatic
     * commit handling of the pooled connection)
     */
    private List<TdsToken> handleTransactions(MssqlProtoContext protoContext, String query, boolean inProc) {
        var upper = query.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        var proxy = (JdbcProxy) protoContext.getProxy();
        var implicit = (boolean) protoContext.getValue("IMPLICIT_TX", false);
        if (upper.contains("SET IMPLICIT_TRANSACTIONS ON")) {
            proxy.executeBegin(protoContext);
            protoContext.setValue("IMPLICIT_TX", true);
            protoContext.setValue("TRANSACTION", true);
            return List.of(statementDone(inProc, 0, false));
        }
        if (upper.contains("SET IMPLICIT_TRANSACTIONS OFF")) {
            protoContext.setValue("IMPLICIT_TX", false);
            protoContext.setValue("TRANSACTION", false);
            proxy.executeCommit(protoContext);
            return List.of(statementDone(inProc, 0, false));
        }
        if (upper.startsWith("IF @@TRANCOUNT > 0 COMMIT TRAN") || upper.startsWith("COMMIT")) {
            proxy.executeCommit(protoContext);
            if (implicit) {
                //Still in implicit transactions mode, next statement begins anew
                proxy.executeBegin(protoContext);
            } else {
                protoContext.setValue("TRANSACTION", false);
            }
            return List.of(statementDone(inProc, 0, false));
        }
        if (upper.startsWith("IF @@TRANCOUNT > 0 ROLLBACK TRAN") || upper.startsWith("ROLLBACK")) {
            proxy.executeRollback(protoContext);
            if (!implicit) {
                proxy.executeCommit(protoContext);
                protoContext.setValue("TRANSACTION", false);
            }
            return List.of(statementDone(inProc, 0, false));
        }
        if (upper.startsWith("BEGIN TRAN")) {
            proxy.executeBegin(protoContext);
            protoContext.setValue("TRANSACTION", true);
            return List.of(statementDone(inProc, 0, false));
        }
        return null;
    }
}
