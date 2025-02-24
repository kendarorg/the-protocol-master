package org.kendar.sql.jdbc;

import org.kendar.iterators.QueryResultIterator;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.sql.parser.SqlStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public abstract class JdbcProxy extends Proxy {
    private static final Logger log = LoggerFactory.getLogger(JdbcProxy.class);
    private final String driver;
    private final String connectionString;
    private final String forcedSchema;
    private final String login;
    private final String password;

    public JdbcProxy(JdbcProtocolSettings settings) {
        super();
        var localDriver = settings.getDriver();
        if (localDriver == null) {
            localDriver = getDefaultDriver();
        }
        this.driver = localDriver;
        this.connectionString = settings.getConnectionString();
        this.forcedSchema = settings.getForceSchema();
        this.login = settings.getLogin();
        this.password = settings.getPassword();
    }

    public JdbcProxy(String driver) {
        this(driver, null, null, null, null);


    }

    public JdbcProxy(String driver, String connectionString, String forcedSchema, String login, String password) {
        super();
        this.driver = driver;
        this.connectionString = connectionString;
        this.forcedSchema = forcedSchema;
        this.login = login;
        this.password = password;

    }

    private static ParametrizedStatement buildParametrizedStatement(String query,
                                                                    List<BindingParameter> parameterValues,
                                                                    SqlStringParser parser, Connection c,
                                                                    ArrayList<JDBCType> concreteTypes, boolean insert) throws SQLException {
        query = parseParameters(query, parser);
        PreparedStatement ps;

        if (query.toLowerCase().startsWith("{call") || query.toLowerCase().startsWith("{? = call")) {
            ps = c.prepareCall(query);
        } else {
            ps = c.prepareStatement(query, insert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        }
        for (var i = 0; i < parameterValues.size(); i++) {
            var pv = parameterValues.get(i);
            if (!pv.isOutput()) {
                ps.setObject(i + 1, convertObject(pv));
            } else {
                if (pv.getValue() == null
                        || pv.getType() == JDBCType.NULL
                ) {
                    ((CallableStatement) ps).registerOutParameter(i + 1, pv.isBinary() ? Types.VARBINARY : Types.VARCHAR);
                } else {
                    ((CallableStatement) ps).registerOutParameter(i + 1, pv.getType());
                }
            }


        }
        return new ParametrizedStatement(query, ps);
    }

    private static Object convertObject(BindingParameter value) {
        if (value == null) return null;
        var val = value.getValue();
        return switch (value.getType()) {
            case INTEGER, BIGINT, TINYINT, SMALLINT -> Long.parseLong(val);
            case BIT, BOOLEAN -> Boolean.parseBoolean(val);
            case BINARY, BLOB, VARBINARY, LONGVARBINARY -> Base64.getDecoder().decode(val);
            case FLOAT -> Float.parseFloat(val);
            case DOUBLE -> Double.parseDouble(val);
            case REAL, NUMERIC -> new BigDecimal(val);
            case TIME, TIME_WITH_TIMEZONE -> Time.valueOf(val);
            case DATE -> {
                if (val.length() > 10) {
                    val = val.substring(0, 10);
                }
                yield Date.valueOf(val);
            }
            case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> Timestamp.valueOf(val);
            default -> val;
        };
    }

    public static String parseParameters(String query, SqlStringParser parser) {
        var parsed = parser.parseString(query);
        StringBuilder realQuery = new StringBuilder();
        for (String item : parsed) {
            if (item.startsWith(parser.getParameterSeparator()) &&
                    item.length() > 1 && item.charAt(1) != '$') {
                realQuery.append("?");
            } else {
                realQuery.append(item);
            }
            realQuery.append(" ");
        }
        query = realQuery.toString().trim();
        return query;
    }

    public static byte[] toBytes(ProxyMetadata field, ResultSet rs, int i) throws SQLException {
        if (!field.isByteData()) {
            if (rs.getString(i) == null) {
                return new byte[]{};
            } else {
                var str = rs.getString(i);
                return str.getBytes(StandardCharsets.UTF_8);
            }
        }

        return rs.getBytes(i);
    }

    public static boolean isByteOut(String clName) {
        var ns = clName.split("\\.");
        var name = ns[ns.length - 1].toLowerCase(Locale.ROOT);
        return switch (name) {
            case ("[b"), ("[c"), ("byte") -> true;
            default -> false;
        };
    }

    private static void fillOutputParametersOnRecordset(List<BindingParameter> parameterValues, PreparedStatement statement, SelectResult result, AtomicLong count) {
        var maxRecordsAtomic = new AtomicLong(1);
        result.setIntResult(false);
        var cs = (CallableStatement) statement;
        for (int i = 0; i < parameterValues.size(); i++) {
            var op = parameterValues.get(i);
            if (!op.isOutput()) continue;
            result.getMetadata().add(new ProxyMetadata(
                    "" + i,
                    op.isBinary()));
        }
        var qrIterator = new QueryResultIterator<List<String>>(
                () -> maxRecordsAtomic.get() > 0,
                () -> {
                    try {
                        var byteRow = new ArrayList<String>();
                        for (int i = 0; i < parameterValues.size(); i++) {
                            var op = parameterValues.get(i);
                            if (!op.isOutput()) continue;
                            if (op.isBinary()) {
                                byteRow.add(Base64.getEncoder().encodeToString((cs.getBytes(i + 1))));
                            } else {
                                byteRow.add(cs.getString(i + 1));
                            }
                        }
                        count.incrementAndGet();
                        maxRecordsAtomic.decrementAndGet();
                        return byteRow;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        while (qrIterator.hasNext()) {
            result.getRecords().add(qrIterator.next());
        }
        result.setCount((int) count.get());
    }

    protected abstract String getDefaultDriver();

    public String getDriver() {
        return driver;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public QueryResultIterator<List<String>> iterateThroughRecSet(final ResultSet resultSet,
                                                                  AtomicLong maxRecordsAtomic,
                                                                  SelectResult result,
                                                                  Statement statement,
                                                                  AtomicLong count) {
        return new QueryResultIterator<>(
                () -> {
                    try {
                        return resultSet.next() && maxRecordsAtomic.get() > 0;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    try {
                        var byteRow = new ArrayList<String>();
                        for (var i = 0; i < result.getMetadata().size(); i++) {
                            var current = result.getMetadata().get(i);
                            if (current.isByteData()) {
                                var data = toBytes(current, resultSet, i + 1);
                                if (data == null) {
                                    data = new byte[0];
                                }
                                byteRow.add(Base64.getEncoder().encodeToString(data));
                            } else {
                                byteRow.add(resultSet.getString(i + 1));
                            }
                        }
                        count.incrementAndGet();
                        maxRecordsAtomic.decrementAndGet();
                        return byteRow;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    try {
                        resultSet.close();
                        statement.close();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public SelectResult executeQuery(int connectionId, boolean insert, String query,
                                     Object connection, int maxRecords,
                                     List<BindingParameter> parameterValues,
                                     SqlStringParser parser,
                                     ArrayList<JDBCType> concreteTypes, ProtoContext context) {

        try {
            var result = new SelectResult();
            query = query.trim();
            long start = System.currentTimeMillis();
            var pluginContext = new PluginContext("JDBC", "QUERY", start, context);
            var jdbcCall = new JdbcCall(query, parameterValues);
            for (var plugin : getPluginHandlers(ProtocolPhase.PRE_CALL, jdbcCall, result)) {
                if (plugin.handle(pluginContext, ProtocolPhase.PRE_CALL, jdbcCall, result)) {
                    return result;
                }
            }

            PreparedStatement statement;

            var c = ((Connection) ((ProxyConnection) connection).getConnection());
            if (parameterValues.isEmpty()) {
                //noinspection SqlSourceToSinkFlow
                statement = c.prepareStatement(jdbcCall.getQuery(), insert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
            } else {
                ParametrizedStatement parametrizedStatementBuilder = buildParametrizedStatement(
                        jdbcCall.getQuery(), parameterValues, parser, c,
                        concreteTypes, insert);
                statement = parametrizedStatementBuilder.ps;
            }

            context.setValue("EXECUTING_NOW", statement);
            var count = new AtomicLong(0);
            if (statement.execute()) {
                runThroughRecordset(maxRecords, statement, result, count);
            } else {
                runThroughSingleResult(insert, parameterValues, statement, result, count);
            }
            for (var plugin : getPluginHandlers(ProtocolPhase.POST_CALL, jdbcCall, result)) {
                if (plugin.handle(pluginContext, ProtocolPhase.POST_CALL, jdbcCall, result)) {
                    break;
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void runThroughSingleResult(boolean insert, List<BindingParameter> parameterValues, PreparedStatement statement, SelectResult result, AtomicLong count) throws SQLException {
        ResultSet resultSet;
        result.setIntResult(true);
        var updateCount = statement.getUpdateCount();

        if (insert) {
            resultSet = statement.getGeneratedKeys();
            if (resultSet != null) {
                result.setIntResult(false);
                result.getMetadata().addAll(identifyFields(resultSet));
                var maxRecordsAtomic = new AtomicLong(Integer.MAX_VALUE);
                var qrIterator = iterateThroughRecSet(resultSet,
                        maxRecordsAtomic,
                        result,
                        statement,
                        count);
                while (qrIterator.hasNext()) {
                    result.getRecords().add(qrIterator.next());
                }

                result.setCount(result.getRecords().size());

            }
        }

        var outputParams = parameterValues.stream().filter(BindingParameter::isOutput).toList();
        if (!outputParams.isEmpty()) {
            fillOutputParametersOnRecordset(parameterValues, statement, result, count);
        } else {
            result.setCount(updateCount);
        }
    }

    private void runThroughRecordset(int maxRecords, PreparedStatement statement, SelectResult result, AtomicLong count) throws SQLException {
        ResultSet resultSet;
        if (maxRecords == 0) {
            maxRecords = Integer.MAX_VALUE;
        }
        var maxRecordsAtomic = new AtomicLong(maxRecords);
        resultSet = statement.getResultSet();

        result.getMetadata().addAll(identifyFields(resultSet));

        var qrIterator = iterateThroughRecSet(resultSet,
                maxRecordsAtomic,
                result,
                statement,
                count);
        while (qrIterator.hasNext()) {
            result.getRecords().add(qrIterator.next());
        }
        result.setCount((int) count.get());
    }

    private List<ProxyMetadata> identifyFields(ResultSet resultSet) {
        try {
            var resultSetMetaData = resultSet.getMetaData();
            var fields = new ArrayList<ProxyMetadata>();
            for (var i = 0; i < resultSetMetaData.getColumnCount(); i++) {

                var isByte = isByteOut(resultSetMetaData.getColumnClassName(i + 1));
                var name = (resultSetMetaData.getColumnLabel(i + 1) == null || resultSetMetaData.getColumnLabel(i + 1).isEmpty()) ?
                        resultSetMetaData.getColumnName(i + 1) : resultSetMetaData.getColumnLabel(i + 1);
                fields.add(new ProxyMetadata(
                        name,
                        isByte,
                        resultSetMetaData.getColumnType(i + 1),
                        resultSetMetaData.getPrecision(i + 1)));
            }
            return fields;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProxyConnection connect(NetworkProtoContext context) {

        try {
            var connection = DriverManager.
                    getConnection(getConnectionString(), getLogin(), getPassword());
            if (this.forcedSchema != null && !this.forcedSchema.isEmpty()) {
                connection.setSchema(this.forcedSchema);
            }
            return new ProxyConnection(connection);
        } catch (SQLException e) {
            log.warn("Error connection `{}`", getConnectionString());
            return new ProxyConnection(null);
        }
    }

    @Override
    public void initialize() {
        if (replayer) return;
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public void executeBegin(ProtoContext protoContext) {
        if (replayer) return;
        try {
            var c = ((Connection) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
            c.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeCommit(ProtoContext protoContext) {
        if (replayer) return;
        try {
            var c = ((Connection) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeRollback(ProtoContext protoContext) {
        if (replayer) return;
        try {
            var c = ((Connection) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
            c.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setIsolation(ProtoContext protoContext, int transactionIsolation) {
        if (replayer) return;
        try {
            var c = ((Connection) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
            c.setTransactionIsolation(transactionIsolation);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
