package org.kendar.proxy;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

public class NetworkProxySocket {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(NetworkProxySocket.class);

    protected final ConcurrentLinkedDeque<BaseEvent> inputQueue = new ConcurrentLinkedDeque<>();
    private final AsynchronousSocketChannel channel;
    private final NetworkProtoContext context;
    private final Semaphore semaphore = new Semaphore(1);
    private final Semaphore readSemaphore = new Semaphore(1);
    private final List<BaseEvent> received = new ArrayList<>();

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
                    BaseEvent fe = received.get(i);
                    if (protoState.canRunEvent(fe)) {
                        founded = fe;
                        log.debug("[PROXY ][RX]: " + mapper.serialize(fe));

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
}
