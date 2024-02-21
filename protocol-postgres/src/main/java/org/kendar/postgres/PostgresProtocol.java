package org.kendar.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kendar.postgres.fsm.*;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.sql.jdbc.DataTypesConverter;
import org.kendar.sql.parser.SqlStringParser;

public class PostgresProtocol extends NetworkProtoDescriptor {
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
    public ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        var result = new PostgresProtoContext(this);
        result.setValue("PARSER", parser);
        return result;
    }


    @Override
    protected void initializeProtocol() {
        addInterruptState(new CancelRequest(BytesEvent.class));
        initialize(
                new ProtoStateSequence(
                        new SSLRequest(BytesEvent.class).asOptional(),
                        new StartupMessage(BytesEvent.class),
                        new ProtoStateWhile(
                                new ProtoStateSwitchCase(
                                        new Query(BytesEvent.class),
                                        new ProtoStateSequence(
                                                new Parse(BytesEvent.class),
                                                new ProtoStateSequence(
                                                        new Bind(BytesEvent.class).asOptional(),
                                                        new Describe(BytesEvent.class).asOptional(),
                                                        new Execute(BytesEvent.class)
                                                ).asOptional()
                                        ),
                                        new ProtoStateSequence(
                                                new Bind(BytesEvent.class),
                                                new Execute(BytesEvent.class)
                                        ),
                                        new Sync(BytesEvent.class),
                                        new Close(BytesEvent.class),
                                        new Terminate(BytesEvent.class)
                                )
                        ),
                        new Terminate(BytesEvent.class)
                )

        );
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
