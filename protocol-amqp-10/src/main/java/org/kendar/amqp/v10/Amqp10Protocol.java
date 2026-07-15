package org.kendar.amqp.v10;

import org.kendar.amqp.v10.context.Amqp10ProtoContext;
import org.kendar.amqp.v10.fsm.Amqp10FrameTranslator;
import org.kendar.amqp.v10.fsm.ProtocolHeader;
import org.kendar.amqp.v10.fsm.events.Amqp10Frame;
import org.kendar.amqp.v10.messages.EmptyFrame;
import org.kendar.amqp.v10.messages.performatives.*;
import org.kendar.amqp.v10.messages.sasl.SaslInit;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.protocol.states.special.Tagged;
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
 * AMQP 1.0 protocol descriptor. Packaged as a pf4j plugin (per
 * protocol.pluginization.md): {@code @Extension} + {@code ExtensionPoint} so the
 * runner's {@code JarPluginManager} can bind it into the DI container.
 * <p>
 * FSM shape follows protocol-amqp-10.md §5.4: header + optional SASL, then
 * {@code open}, a per-session ({@code SESSION:<channel>}) loop of
 * begin, then (attach/flow/transfer/disposition/detach/empty) repeated, then end,
 * then {@code close}.
 */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10Protocol extends NetworkProtoDescriptor implements ExtensionPoint {

    private static final boolean IS_BIG_ENDIAN = true;
    private static final int PORT = 5672;
    final AtomicBoolean running = new AtomicBoolean(true);
    private final Logger log = LoggerFactory.getLogger(Amqp10Protocol.class);
    private final int port;
    private TimerInstance timer;

    @TpmConstructor
    public Amqp10Protocol(GlobalSettings ini, Amqp10ProtocolSettings settings, Amqp10Proxy proxy,
                          @TpmNamed(tags = "amqp10") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());
    }

    public Amqp10Protocol(int port) {
        this.port = port;
        var pp = new Amqp10ProtocolSettings();
        pp.setPort(port);
        setSettings(pp);
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new EmptyFrame(Amqp10Frame.class));
        addInterruptState(new Amqp10FrameTranslator(BytesEvent.class));

        initialize(
                new ProtoStateSequence(
                        new ProtocolHeader(BytesEvent.class),
                        new SaslInit(Amqp10Frame.class),               // optional (SASL path)
                        new Open(Amqp10Frame.class),
                        new Tagged(
                                Tag.ofKeys("SESSION"),
                                new ProtoStateWhile(
                                        new ProtoStateSequence(
                                                new Begin(Amqp10Frame.class),
                                                new ProtoStateWhile(
                                                        new ProtoStateSwitchCase(
                                                                new Attach(Amqp10Frame.class),
                                                                new Flow(Amqp10Frame.class),
                                                                new Transfer(Amqp10Frame.class),
                                                                new Disposition(Amqp10Frame.class),
                                                                new Detach(Amqp10Frame.class),
                                                                new EmptyFrame(Amqp10Frame.class)
                                                        )
                                                ),
                                                new End(Amqp10Frame.class)
                                        ))
                        ),
                        new Close(Amqp10Frame.class)
                )
        );
    }

    public void start() {
        if (timer != null) {
            timer.cancel();
        }
        var timerService = new TimerService();
        timer = timerService.schedule(this::sendHeartbeat, 1000, 5 * 1000);
    }

    private void sendHeartbeat() {
        var toRemove = new java.util.ArrayList<Integer>();
        getContextsCache().forEach((key, value) -> {
            var ctx = (Amqp10ProtoContext) value;
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
        return new Amqp10ProtoContext(this, contextId);
    }
}
