package org.kendar.amqp.v09;

import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.AmqpFrameTranslator;
import org.kendar.amqp.v09.fsm.ProtocolHeader;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.frames.HearthBeatFrame;
import org.kendar.amqp.v09.messages.methods.basic.*;
import org.kendar.amqp.v09.messages.methods.channel.ChannelClose;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionClose;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionStartOk;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionTuneOk;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeBind;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeDeclare;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeDelete;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeUnbind;
import org.kendar.amqp.v09.messages.methods.queue.*;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.protocol.states.special.Tagged;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.TimerInstance;
import org.kendar.utils.TimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TpmService(tags = "amqp091")
public class AmqpProtocol extends NetworkProtoDescriptor {

    private static final boolean IS_BIG_ENDIAN = true;
    private static final int PORT = 5672;
    final AtomicBoolean running = new AtomicBoolean(true);
    private final Logger log = LoggerFactory.getLogger(AmqpProtocol.class);
    private int port = PORT;
    private TimerInstance timer;

    @TpmConstructor
    public AmqpProtocol(GlobalSettings ini,ByteProtocolSettingsWithLogin settings, AmqpProxy proxy,
                        @TpmNamed(tags = "amqp091") List<ProtocolPluginDescriptor> plugins) {
        this.setTimeout(settings.getTimeoutSeconds());
        this.setSettings(settings);
        for (var i = plugins.size() - 1; i >= 0; i--) {
            var plugin = plugins.get(i);
            var specificPluginSetting = settings.getPlugin(plugin.getId(), plugin.getSettingClass());
            if (specificPluginSetting != null) {
                plugin.initialize(ini, settings, specificPluginSetting);
                plugin.refreshStatus();
            } else {
                plugins.remove(i);
            }
        }
        proxy.setPlugins(plugins);
        this.setProxy(proxy);
    }

    public AmqpProtocol(int port) {
        this.port = port;
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new HearthBeatFrame(AmqpFrame.class));
        addInterruptState(new AmqpFrameTranslator(BytesEvent.class));

        initialize(
                new ProtoStateSequence(
                        new ProtocolHeader(BytesEvent.class),
                        new ConnectionStartOk(AmqpFrame.class),
                        new ConnectionTuneOk(AmqpFrame.class),
                        new ConnectionOpen(AmqpFrame.class),
                        new Tagged(
                                Tag.ofKeys("CHANNEL"),
                                new ProtoStateWhile(
                                        new ProtoStateSequence(
                                                new ChannelOpen(AmqpFrame.class),
                                                new ProtoStateWhile(
                                                        new ProtoStateSwitchCase(
                                                                new HearthBeatFrame(AmqpFrame.class),
                                                                new QueueDeclare(AmqpFrame.class),
                                                                new QueueBind(AmqpFrame.class),
                                                                new QueueUnbind(AmqpFrame.class),
                                                                new QueuePurge(AmqpFrame.class),
                                                                new QueueDelete(AmqpFrame.class),
                                                                new ExchangeDeclare(AmqpFrame.class),
                                                                new ExchangeBind(AmqpFrame.class),
                                                                new ExchangeUnbind(AmqpFrame.class),
                                                                new ExchangeDelete(AmqpFrame.class),
                                                                new BasicConsume(AmqpFrame.class),
                                                                new BasicCancel(AmqpFrame.class),
                                                                new BasicGet(AmqpFrame.class),
                                                                new ProtoStateSequence(
                                                                        new BasicPublish(AmqpFrame.class),
                                                                        new HeaderFrame(AmqpFrame.class),
                                                                        new BodyFrame(AmqpFrame.class)
                                                                ),
                                                                new BasicAck(AmqpFrame.class),
                                                                new BasicNack(AmqpFrame.class),
                                                                new Reject(AmqpFrame.class)
                                                        )
                                                ),
                                                new ChannelClose(AmqpFrame.class)
                                        ))
                        ),
                        new ConnectionClose(AmqpFrame.class)
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
        var toRemove = new ArrayList<Integer>();
        getContextsCache().forEach((key, value) -> {
            var ctx = (AmqpProtoContext) value;
            try {
                if (ctx.getValue("HEARTBEAT") == null) return;
                var hb = (short) ctx.getValue("HEARTBEAT");
                if (hb <= 0) return;
                var lastHb = (long) ctx.getValue("HEARTBEAT_LAST");
                if (ctx.isConnected() && lastHb < (hb + getNow())) {
                    var hbf = new HearthBeatFrame();
                    hbf.setChannel((short) 0);
                    hbf.setProtoDescriptor(this);
                    ctx.write(hbf);
                }

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
        timer.cancel();
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
        return new AmqpProtoContext(this, contextId);
    }
}
