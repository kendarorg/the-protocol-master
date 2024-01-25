package org.kendar.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kendar.postgres.fsm.*;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.protocol.fsm.Start;
import org.kendar.server.Channel;
import org.kendar.sql.jdbc.DataTypesConverter;
import org.kendar.sql.parser.SqlStringParser;

public class PostgresProtocol extends ProtoDescriptor {
    private static final int PORT = 5432;
    private static final boolean IS_BIG_ENDIAN = true;
    private static final SqlStringParser parser = new SqlStringParser("$");
    private static DataTypesConverter dataTypesConverter;

    static {
        try {
            var om = new ObjectMapper();
            String text = new String(PostgresProtocol.class.getResourceAsStream("/postgresdtt.json")
                    .readAllBytes());
            dataTypesConverter = new DataTypesConverter(om.readValue(text, new TypeReference<>() {
            }));
        } catch (Exception e) {

        }
    }

    private final int port;

    public PostgresProtocol() {
        this(PORT);
    }

    public PostgresProtocol(int port) {
        this.port = port;
    }

    public static DataTypesConverter getDataTypesConverter() {
        return dataTypesConverter;
    }

    @Override
    public ProtoContext createContext(ProtoDescriptor protoDescriptor, Channel client) {
        var result = new PostgresProtoContext(this, client);
        result.setValue("PARSER", parser);
        return result;
    }

    @Override
    protected void initializeProtocol() {
        addState(new Start(),
                new SSLRequest(BytesEvent.class),
                new StartupMessage(BytesEvent.class));


        addState(new SSLRequest(BytesEvent.class),
                new StartupMessage(BytesEvent.class));

        addState(new StartupMessage(BytesEvent.class),
                new Query(BytesEvent.class),
                new Parse(BytesEvent.class),
                new Terminate(BytesEvent.class));

        addState(new Parse(BytesEvent.class),
                new Bind(BytesEvent.class),
                new Sync(BytesEvent.class),
                new Terminate(BytesEvent.class));

        addState(new Bind(BytesEvent.class),
                new Describe(BytesEvent.class),
                new Execute(BytesEvent.class),
                new Terminate(BytesEvent.class));

        addState(new Describe(BytesEvent.class),
                new Execute(BytesEvent.class),
                new Terminate(BytesEvent.class));

        addState(new Execute(BytesEvent.class),
                new Bind(BytesEvent.class),
                new Sync(BytesEvent.class),
                new Terminate(BytesEvent.class),
                new Parse(BytesEvent.class));

        addState(new Sync(BytesEvent.class),
                new Query(BytesEvent.class),
                new Parse(BytesEvent.class),
                new Bind(BytesEvent.class),
                new Terminate(BytesEvent.class),
                new Close(BytesEvent.class));

        addState(new Close(BytesEvent.class),
                new Query(BytesEvent.class),
                new Parse(BytesEvent.class),
                new Bind(BytesEvent.class),
                new Terminate(BytesEvent.class));

        addState(new Query(BytesEvent.class),
                new Parse(BytesEvent.class),
                new Query(BytesEvent.class),
                new Terminate(BytesEvent.class));
    }

    @Override
    public boolean isBe() {
        return IS_BIG_ENDIAN;
    }

    @Override
    public int getPort() {
        return port;
    }
}
