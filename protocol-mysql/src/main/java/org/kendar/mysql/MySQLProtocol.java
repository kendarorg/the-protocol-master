package org.kendar.mysql;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.mysql.executor.MySQLExecutor;
import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.*;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.NetworkWait;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.settings.GlobalSettings;
import org.kendar.sql.parser.SqlStringParser;

import java.util.List;

@TpmService(tags = "mysql")
public class MySQLProtocol extends NetworkProtoDescriptor {

    private static final SqlStringParser parser = new SqlStringParser("?");
    private static final int PORT = 3306;
    private static final boolean IS_BIG_ENDIAN = true;
    private final int port;
    private MySQLExecutor executor = new MySQLExecutor();

    @TpmConstructor
    public MySQLProtocol(GlobalSettings ini, MySqlProtocolSettings settings, MySQLProxy proxy,
                         @TpmNamed(tags = "mysql") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());

    }

    public MySQLProtocol() {
        this(PORT);
    }

    public MySQLProtocol(int port) {
        this.port = port;
        setSettings(new MySqlProtocolSettings());
    }

    @Override
    public boolean sendImmediateGreeting() {

        return true;
    }

    @Override
    public boolean isBe() {
        return IS_BIG_ENDIAN;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor,
                                         int contextId) {
        var result = new MySQLProtoContext(this, contextId);
        result.setExecutor(this.executor);
        result.setValue("PARSER", parser);
        return result;
    }

    @Override
    protected void initializeProtocol() {
        initialize(new ProtoStateSequence(
                new ConnectionEstablished(BytesEvent.class),
                new Auth(BytesEvent.class),
                new ProtoStateWhile(
                        new NetworkWait(BytesEvent.class).asOptional()
                ),
                new ProtoStateWhile(
                        new Command(BytesEvent.class),
                        new ProtoStateSwitchCase(
                                new ComQuery(CommandEvent.class),
                                new ComStmtPrepare(CommandEvent.class),
                                new ComStmtExecute(CommandEvent.class),
                                new ComRefresh(CommandEvent.class),
                                new ComInitDb(CommandEvent.class),
                                new ComPing(CommandEvent.class),
                                new ComQuit(CommandEvent.class)
                        )
                )));
    }
}
