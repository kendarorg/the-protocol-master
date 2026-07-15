package org.kendar.kafka;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.fsm.ApiVersionsRequest;
import org.kendar.kafka.fsm.DescribeClusterRequest;
import org.kendar.kafka.fsm.FindCoordinatorRequest;
import org.kendar.kafka.fsm.GenericRequest;
import org.kendar.kafka.fsm.KafkaFrameTranslator;
import org.kendar.kafka.fsm.MetadataRequest;
import org.kendar.kafka.fsm.events.KafkaRequestEvent;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.TimerInstance;
import org.kendar.utils.TimerService;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apache Kafka protocol descriptor, packaged as a pf4j plugin (per
 * protocol.pluginization.md): {@code @Extension} + {@code ExtensionPoint} so the
 * runner's {@code JarPluginManager} binds it into DI.
 * <p>
 * Kafka is strictly client-initiated request/response with in-order responses
 * per connection and no server push, so the FSM is flat: a size-prefixed frame
 * translator (interrupt) feeds a {@code while(switch-case)} of request states,
 * each forwarding to the broker and expecting a correlation-id-matched response
 * (protocol-kafka.md §6.3). The catch-all {@link GenericRequest} is LAST.
 */
@Extension
@TpmService(tags = "kafka")
public class KafkaProtocol extends NetworkProtoDescriptor implements ExtensionPoint {

    private static final boolean IS_BIG_ENDIAN = true;
    final AtomicBoolean running = new AtomicBoolean(true);
    private final Logger log = LoggerFactory.getLogger(KafkaProtocol.class);
    private final int port;
    private final KafkaProtocolSettings settings;
    private TimerInstance timer;

    @TpmConstructor
    public KafkaProtocol(GlobalSettings ini, KafkaProtocolSettings settings, KafkaProxy proxy,
                         @TpmNamed(tags = "kafka") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.settings = settings;
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());
    }

    public KafkaProtocol(int port) {
        this.port = port;
        var pp = new KafkaProtocolSettings();
        pp.setPort(port);
        setSettings(pp);
        this.settings = pp;
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new KafkaFrameTranslator(BytesEvent.class));
        initialize(
                new ProtoStateWhile(
                        new ProtoStateSwitchCase(
                                new ApiVersionsRequest(KafkaRequestEvent.class),
                                new MetadataRequest(KafkaRequestEvent.class),
                                new FindCoordinatorRequest(KafkaRequestEvent.class),
                                new DescribeClusterRequest(KafkaRequestEvent.class),
                                new GenericRequest(KafkaRequestEvent.class)   // catch-all LAST
                        )
                )
        );
    }

    public String getAdvertisedHost() {
        return settings.getAdvertisedHost();
    }

    public void start() {
        if (timer != null) {
            timer.cancel();
        }
        var timerService = new TimerService();
        timer = timerService.schedule(this::reapDeadConnections, 1000, 5 * 1000);
    }

    private void reapDeadConnections() {
        var toRemove = new java.util.ArrayList<Integer>();
        getContextsCache().forEach((key, value) -> {
            var ctx = (KafkaContext) value;
            try {
                if (!ctx.isConnected()) {
                    toRemove.add(key);
                }
            } catch (Exception e) {
                toRemove.add(key);
            }
        });
        for (var key : toRemove) {
            getContextsCache().remove(key);
        }
    }

    @Override
    public void terminate() {
        running.set(false);
        if (timer != null) {
            timer.cancel();
        }
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
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId) {
        return new KafkaContext(this, contextId);
    }
}
