package org.kendar.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.postgres.executor.PostgresExecutor;
import org.kendar.postgres.fsm.*;
import org.kendar.postgres.fsm.events.PostgresPacket;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.settings.GlobalSettings;
import org.kendar.sql.jdbc.DataTypesConverter;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@TpmService(tags = "postgres")
public class PostgresProtocol extends NetworkProtoDescriptor {
    private static final Logger log = LoggerFactory.getLogger(PostgresProtocol.class);
    private static final int PORT = 5432;
    private static final boolean IS_BIG_ENDIAN = true;
    private static final SqlStringParser parser = new SqlStringParser("$");
    private static DataTypesConverter dataTypesConverter;

    static {
        try {
            String text = new String(PostgresProtocol.class.getResourceAsStream("/postgresdtt.json")
                    .readAllBytes());
            dataTypesConverter = new DataTypesConverter(new JsonMapper().deserialize(text, new TypeReference<>() {
            }));
        } catch (Exception e) {
            log.trace("Ignorable", e);
        }
    }

    private final int port;
    private PostgresExecutor executor = new PostgresExecutor();

    @TpmConstructor
    public PostgresProtocol(GlobalSettings ini, PostgresProtocolSettings settings, PostgresProxy proxy,
                            @TpmNamed(tags = "postgres") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());
    }

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
    public ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId) {
        var result = new PostgresProtoContext(this, contextId);
        result.setExecutor(this.executor);
        result.setValue("PARSER", parser);
        return result;
    }


    @Override
    protected void initializeProtocol() {
        PostgresProtoContext.initializePids();
        addInterruptState(new CancelRequest(BytesEvent.class));
        addInterruptState(new PostgresPacketTranslator(BytesEvent.class));
        initialize(
                new ProtoStateSequence(
                        new SSLRequest(BytesEvent.class).asOptional(),
                        new StartupMessage(BytesEvent.class),
                        new ProtoStateWhile(
                                new ProtoStateSwitchCase(
                                        new Query(PostgresPacket.class),
                                        new ProtoStateSequence(
                                                new Parse(PostgresPacket.class),
                                                new ProtoStateSequence(
                                                        new Bind(PostgresPacket.class).asOptional(),
                                                        new Describe(PostgresPacket.class).asOptional(),
                                                        new Execute(PostgresPacket.class)
                                                ).asOptional()
                                        ),
                                        new ProtoStateSequence(
                                                new Bind(PostgresPacket.class),
                                                new Execute(PostgresPacket.class)
                                        ),
                                        new Sync(PostgresPacket.class),
                                        new Close(PostgresPacket.class),
                                        new Terminate(PostgresPacket.class)
                                )
                        ),
                        new Terminate(PostgresPacket.class)
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
