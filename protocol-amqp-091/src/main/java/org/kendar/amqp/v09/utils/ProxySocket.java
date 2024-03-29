package org.kendar.amqp.v09.utils;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.GenericFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.*;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionBlocked;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionUnblocked;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ProxySocket {
    private static final Logger log = LoggerFactory.getLogger(ProxySocket.class.getName());


    private final AsynchronousSocketChannel channel;
    private final NetworkProtoContext context;
    private final Semaphore semaphore = new Semaphore(1);
    private final Semaphore readSemaphore = new Semaphore(1);
    private final List<BytesEvent> received = new ArrayList<BytesEvent>();
    private final List<ProtoState> states = new ArrayList<>(Arrays.asList(
            new BasicDeliver(),
            new BasicCancel().asProxy(),
            new HeaderFrame().asProxy(),
            new BodyFrame().asProxy(),
            new ConnectionBlocked().asProxy(),
            new ConnectionUnblocked().asProxy(),
            new BasicAck().asProxy(),
            new BasicNack().asProxy(),
            new BasicReturn().asProxy(),
            new BasicGetEmpty().asProxy()));
    protected ConcurrentLinkedDeque<BytesEvent> inputQueue = new ConcurrentLinkedDeque<BytesEvent>();

    public ProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        this.context = context;
        try {
            channel = AsynchronousSocketChannel.open(group);
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);


            BBuffer tempBuffer = context.buildBuffer();
            channel.connect(inetSocketAddress, channel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
                @Override
                public void completed(Void result, AsynchronousSocketChannel channel) {
                    //start to read message
                    final ByteBuffer buffer = ByteBuffer.allocate(4096);

                    channel.read(buffer, 30000, TimeUnit.MILLISECONDS, buffer, new CompletionHandler<>() {

                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                                Iterator<ProtoStep> stepsToInvoke = null;
                                ProtoState possible = null;
                                //message is read from server
                                attachment.flip();
                                if (result != -1 || attachment.remaining() > 0) {
                                    var byteArray = new byte[attachment.remaining()];
                                    attachment.get(byteArray);

                                    try {
                                        semaphore.acquire();
                                        tempBuffer.setPosition(tempBuffer.size());
                                        tempBuffer.write(byteArray);
                                        tempBuffer.setPosition(0);

                                        log.trace("[PROXY ][RX] Bytes: " + byteArray.length);
                                        var gf = new GenericFrame();
                                        var be = new BytesEvent(context, null, tempBuffer);
                                        var fre = new AmqpFrame(context, null, tempBuffer, tempBuffer.getShort(1));
                                        boolean run = true;
                                        while (run) {
                                            run = false;
                                            for (int i = 0; i < states.size(); i++) {
                                                possible = states.get(i);
                                                if (possible.canRunEvent(be)) {
                                                    stepsToInvoke = possible.executeEvent(be);
                                                    tempBuffer.truncate();
                                                    context.runSteps(stepsToInvoke, possible, be);
                                                    log.trace("[PROXY ][RX] Found(1): " + possible.getClass().getSimpleName());
                                                    run = true;
                                                    break;
                                                } else if (possible.canRunEvent(fre)) {
                                                    stepsToInvoke = possible.executeEvent(fre);
                                                    tempBuffer.truncate();
                                                    context.runSteps(stepsToInvoke, possible, fre);
                                                    log.trace("[PROXY ][RX] Found(1): " + possible.getClass().getSimpleName());
                                                    run = true;
                                                    break;
                                                }
                                            }
                                            if (!run && gf.canRun(be)) {
                                                var event = gf.execute(be);
                                                log.trace("[PROXY ][RX] Found(2): " + gf.getClass().getSimpleName());
                                                inputQueue.add(event);
                                                tempBuffer.truncate();
                                                run = true;
                                            }
                                        }
                                        semaphore.release();

                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                attachment.clear();
                                if (!channel.isOpen()) {
                                    return;
                                }
                                channel.read(buffer, 30000, TimeUnit.MILLISECONDS, buffer, this);
                            }

                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer buffer) {
                            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                                log.trace("[PROXY ][RX] Fail to read message from server", exc);
                            }
                        }

                    });
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                        log.error("[PROXY ] Fail to connect to server", exc);
                    }
                }

            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(BBuffer buffer) {
        buffer.setPosition(0);
        channel.write(ByteBuffer.wrap(buffer.getAll()));
    }

    public void write(ReturnMessage rm, BBuffer buffer) {
        var returnMessage = (NetworkReturnMessage) rm;
        buffer.setPosition(0);
        buffer.truncate(0);
        returnMessage.write(buffer);
        write(buffer);
        log.debug("[PROXY ][TX]: " + returnMessage.getClass().getSimpleName());
    }

    public List<ReturnMessage> read(ProtoState protoState) {

        log.debug("[SERVER][??]: " + protoState.getClass().getSimpleName());
        BaseEvent founded = null;
        try {
            while (founded == null) {
                readSemaphore.acquire();

                while (!inputQueue.isEmpty()) {
                    var toAdd = inputQueue.poll();
                    if (toAdd == null) break;
                    received.add(toAdd);
                }
                for (int i = 0; i < received.size(); i++) {
                    BytesEvent fr = received.get(i);
                    fr = new BytesEvent(context, null, fr.getBuffer());
                    var fe = new AmqpFrame(context, null, fr.getBuffer(), (short) -1);
                    //If can run the
                    if (protoState.canRunEvent(fr)) {
                        founded = fr;
                        received.remove(i);
                        break;
                    } else if (protoState.canRunEvent(fe)) {
                        founded = fe;
                        received.remove(i);
                        break;
                    }

                }
                readSemaphore.release();
                Sleeper.yield();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var returnMessage = new ArrayList<ReturnMessage>();
        Iterator<ProtoStep> it = protoState.executeEvent(founded);
        while (it.hasNext()) {
            returnMessage.add(it.next().run());
        }
        log.debug("[PROXY ][RX]: " + protoState.getClass().getSimpleName());
        return returnMessage;
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {

        }
    }
}
