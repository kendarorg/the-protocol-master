package org.kendar.mssql;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.JdbcProtocol;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.mssql.executor.MssqlExecutor;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.*;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
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
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.List;

@Extension
@TpmService(tags = "mssql")
public class MssqlProtocol extends JdbcProtocol implements ExtensionPoint {
    private static final Logger log = LoggerFactory.getLogger(MssqlProtocol.class);
    private static final int PORT = 1433;
    private static final boolean IS_BIG_ENDIAN = true;
    private static final SqlStringParser parser = new SqlStringParser("@");
    private static DataTypesConverter dataTypesConverter;

    static {
        try {
            String text = new String(MssqlProtocol.class.getResourceAsStream("/mssqldtt.json")
                    .readAllBytes());
            dataTypesConverter = new DataTypesConverter(new JsonMapper().deserialize(text, new TypeReference<>() {
            }));
        } catch (Exception e) {
            log.trace("Ignorable", e);
        }
    }

    private final MssqlExecutor executor = new MssqlExecutor();
    private final int port;

    @TpmConstructor
    public MssqlProtocol(GlobalSettings ini, MssqlProtocolSettings settings, MssqlProxy proxy,
                         @TpmNamed(tags = "mssql") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());
    }

    public MssqlProtocol() {
        this(PORT);
    }

    public MssqlProtocol(int port) {
        super(port);
        this.port = port;
        var pp = new MssqlProtocolSettings();
        pp.setPort(port);
        setSettings(pp);
    }

    public static DataTypesConverter getDataTypesConverter() {
        return dataTypesConverter;
    }

    @Override
    public ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId) {
        var result = new MssqlProtoContext(this, contextId);
        result.setExecutor(this.executor);
        result.setValue("PARSER", parser);
        return result;
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new TdsPacketTranslator(BytesEvent.class));
        addInterruptState(new Attention(TdsPacket.class));
        initialize(
                new ProtoStateSequence(
                        new PreLogin(TdsPacket.class),
                        new TdsSslHandshake(TdsPacket.class).asOptional(),
                        new Login7(TdsPacket.class),
                        new ProtoStateWhile(
                                new ProtoStateSwitchCase(
                                        new SqlBatch(TdsPacket.class),
                                        new Rpc(TdsPacket.class),
                                        new TransactionManager(TdsPacket.class)
                                )
                        )
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
