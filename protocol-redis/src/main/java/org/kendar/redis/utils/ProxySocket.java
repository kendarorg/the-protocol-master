package org.kendar.redis.utils;


import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.redis.fsm.Resp3MessageTranslator;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.utils.JsonMapper;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ProxySocket {
    private static final Logger log = LoggerFactory.getLogger(ProxySocket.class.getName());
    private static final JsonMapper mapper = new JsonMapper();
    protected final ConcurrentLinkedDeque<Resp3Message> inputQueue = new ConcurrentLinkedDeque<>();
    private final AsynchronousSocketChannel channel;
    private final NetworkProtoContext context;
    private final Semaphore semaphore = new Semaphore(1);
    private final Semaphore readSemaphore = new Semaphore(1);
    private final List<Resp3Message> received = new ArrayList<>();

    public ProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group, Resp3Storage storage) {
        this.context = context;
        try {
            channel = AsynchronousSocketChannel.open(group);
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            var possible = new Resp3MessageTranslator().asProxy();

            BBuffer tempBuffer = context.buildBuffer();
            channel.connect(inetSocketAddress, channel, new CompletionHandler<>() {
                @Override
                public void completed(Void result, AsynchronousSocketChannel channel) {
                    //start to read message
                    final ByteBuffer buffer = ByteBuffer.allocate(4096);

                    channel.read(buffer, 30000, TimeUnit.MILLISECONDS, buffer, new CompletionHandler<>() {

                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {

                            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                                Iterator<ProtoStep> stepsToInvoke = null;
                                //ProtoState possible = null;
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
                                        //var gf = new GenericFrame();
                                        var be = new BytesEvent(context, null, tempBuffer);
                                        var fre = new Resp3Message(context, null, new ArrayList<>(), "");
                                        boolean run = true;
                                        while (run) {
                                            run = false;
                                            if (possible.canRunEvent(be)) {
                                                stepsToInvoke = possible.executeEvent(be);
                                                tempBuffer.truncate();
                                                if (stepsToInvoke.hasNext()) {
                                                    var sub = stepsToInvoke.next();
                                                    var returnedMessage = (Resp3Message) sub.run();
                                                    if (returnedMessage.getData() instanceof List) {
                                                        var list = (List) returnedMessage.getData();
                                                        if (!list.isEmpty() && list.get(0) != null) {
                                                            if ("message".equalsIgnoreCase(list.get(0).toString())) {
                                                                log.trace("[PROXY ][RX] Found(2): " + returnedMessage.getMessage());
                                                                var res = "";
                                                                res = "{\"type\":\"RESPONSE\",\"data\":" + mapper.serialize(returnedMessage.getData()) + "}";
                                                                var jsonRes = mapper.toJsonNode(res);
                                                                storage.write(
                                                                        context.getContextId(),
                                                                        null,
                                                                        jsonRes,
                                                                        0, "RESPONSE", "RESP3"
                                                                );

                                                                context.write(returnedMessage);
                                                                run = true;


                                                                break;
                                                            }

                                                        }
                                                    }
                                                    inputQueue.add(returnedMessage);
                                                    log.trace("[PROXY ][RX] Found(1): " + returnedMessage.getMessage());
                                                }

                                                run = true;
                                                break;
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
                    Resp3Message fe = received.get(i);
                    if (protoState.canRunEvent(fe)) {
                        founded = fe;
                        log.debug("[PROXY ][RX]: " + mapper.serialize(fe.getData()));

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
