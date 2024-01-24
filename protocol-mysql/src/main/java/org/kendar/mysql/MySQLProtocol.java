package org.kendar.mysql;

import org.kendar.mysql.executor.MySQLProtoContext;
import org.kendar.mysql.fsm.*;
import org.kendar.mysql.fsm.events.CommandEvent;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.protocol.fsm.Start;
import org.kendar.server.Channel;
import org.kendar.sql.parser.SqlStringParser;


public class MySQLProtocol extends ProtoDescriptor {


    private static final SqlStringParser parser = new SqlStringParser("?");

    private static final int PORT = 3306;
    private static final boolean IS_BIG_ENDIAN = true;
    private final int port;

    public MySQLProtocol() {
        this(PORT);
    }

    public MySQLProtocol(int port) {
        this.port = port;
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
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, Channel client) {
        var result = new MySQLProtoContext(this, client);
        result.setValue("PARSER", parser);
        return result;
    }

    @Override
    protected void initializeProtocol() {
        addState(new Start(),
                new ConnectionEstablished(BytesEvent.class));

        addState(new ConnectionEstablished(),
                new Auth(BytesEvent.class));

        addState(new Auth(),
                new Command(BytesEvent.class));

        addState(new Command(),
                new ComQuery(CommandEvent.class),
                new ComRefresh(CommandEvent.class),
                new ComQuit(CommandEvent.class),
                new ComInitDb(CommandEvent.class),
                new ComPing(CommandEvent.class),
                new ComStmtPrepare(CommandEvent.class),
                new ComStmtExecute(CommandEvent.class));

        addState(new ComStmtPrepare(),
                new Command(BytesEvent.class));
        addState(new ComRefresh(),
                new Command(BytesEvent.class));
        addState(new ComQuery(),
                new Command(BytesEvent.class));
        addState(new ComInitDb(),
                new Command(BytesEvent.class));
        addState(new ComPing(),
                new Command(BytesEvent.class));
        addState(new ComStmtExecute(),
                new Command(BytesEvent.class));
    }
}
