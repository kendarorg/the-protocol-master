package org.kendar.mssql;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.ProxyException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.sql.jdbc.JdbcProxy;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;

@Extension
@TpmService
public class MssqlProxy extends JdbcProxy implements ExtensionPoint {
    private static final Logger log = LoggerFactory.getLogger(MssqlProxy.class);
    private final String forcedSchema;

    @TpmConstructor
    public MssqlProxy(MssqlProtocolSettings settings) {
        super(settings);
        this.forcedSchema = settings.getForceSchema();
    }

    public MssqlProxy(String driver) {
        super(driver);
        this.forcedSchema = null;
    }

    public MssqlProxy(String driver, String connectionString, String forcedSchema, String login, String password) {
        super(driver, connectionString, forcedSchema, login, password);
        this.forcedSchema = forcedSchema;
    }

    @Override
    protected String getDefaultDriver() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    /*
     * The SQLServer driver is bundled inside the plugin jar, so it is only
     * visible to the plugin classloader: driver loading and DriverManager
     * calls must originate from plugin-loaded code, not from JdbcProxy in
     * protocol-common-jdbc (parent classloader), or DriverManager would
     * refuse the driver ("No suitable driver").
     */
    @Override
    public void initialize() {
        if (isReplayer()) return;
        try {
            if (getDriver() != null && !getDriver().isEmpty()) {
                Class.forName(getDriver());
            }
        } catch (ClassNotFoundException e) {
            throw new ProxyException(e);
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
}
