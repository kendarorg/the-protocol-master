package org.kendar.protocol.context;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.exceptions.ConnectionExeception;
import org.kendar.exceptions.UnknownCommandException;
import org.kendar.exceptions.UnknownHandlerException;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.NullState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.Proxy;
import org.kendar.server.ClientServerChannel;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;

public class NetworkProtoContext extends ProtoContext {
    private static final Logger log = LoggerFactory.getLogger(NetworkProtoContext.class);

    private final Thread senderThread;
    private boolean greetingsSent = false;
    private ClientServerChannel client;
    private final int MAX_REMINDER_TESTS = 20;
    private Proxy proxy;
    private BytesEvent remainingBytes = null;
    private int unchangedDataTest = MAX_REMINDER_TESTS;
    private int lastRemainingBytesSize = -1;
    private BBuffer resultBuffer;
    private final ConcurrentLinkedDeque<NetworkStep> outputQueue = new ConcurrentLinkedDeque<>();

    public NetworkProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
        this.senderThread = new Thread(this::senderThread);
        senderThread.start();
    }

    @Override
    public void write(ReturnMessage rm) {
        var returnMessage = (NetworkReturnMessage) rm;
        resultBuffer.reset();
        returnMessage.write(resultBuffer);
        var length = resultBuffer.size();
        var response = ByteBuffer.allocate(length);
        response.put(resultBuffer.toArray());
        response.flip();
        var res = client.write(response);
        if (res != null) {
            try {
                res.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("[SERVER][TX] Cannot write message: " + returnMessage.getClass().getSimpleName() + " " + e.getMessage());
                throw new ConnectionExeception("CANNOT REASSIGN CHANNEL");
            }
        }
    }

    public void setClient(ClientServerChannel client) {
        if (this.client == null) {
            this.client = client;
        } else {
            log.error("[SERVER] Cannot reassign channel");
            throw new ConnectionExeception("Cannot reassign channel");
        }
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void preStart() {
        remainingBytes = new BytesEvent(this, null, buildBuffer());
        super.preStart();
    }

    @Override
    protected ProtoState runInternal(BaseEvent event) {
        if (executionStack.empty()) {
            var message = "Missing event handler for " + event.getClass().getSimpleName();
            if (event instanceof BytesEvent) {
                if (((BytesEvent) event).getBuffer().size() == 0) {
                    return null;
                }
                var size = Math.min(remainingBytes.getBuffer().size(), 20);

                message += " " + BBuffer.toHexByteArray(remainingBytes.getBuffer().getBytes(size));
            }


            log.error("[SERVER][EX] Unknown: " + message);
            throw new UnknownCommandException("Unknown command issued: "+message);
        }
        if (event instanceof BytesEvent) {
            if (((BytesEvent) event).getBuffer().size() == 0) {
                if (greetingsSent || !((NetworkProtoDescriptor) descriptor).sendImmediateGreeting()) {
                    try{
                        return null;
                    }finally {
                        Sleeper.yield(10);
                    }

                } else if (!greetingsSent) {
                    greetingsSent = true;
                }
            }
        }
        return super.runInternal(event);
    }

    @Override
    protected boolean customHandledEvent(BaseEvent event) {
        if (event instanceof BytesEvent) {
            var be = (BytesEvent) event;
            remainingBytes.getBuffer().append(be.getBuffer());
            unchangedDataTest = MAX_REMINDER_TESTS;
            return true;
        }
        return false;
    }

    @Override
    protected BaseEvent findCurrentEvent() {
        if (!orderedEvents.isEmpty()) {
            unchangedDataTest = MAX_REMINDER_TESTS;
            var lastEvent = orderedEvents.remove(0);
            if(lastEvent instanceof BytesEvent){
                var rbSize = remainingBytes.getBuffer().size();
                remainingBytes.getBuffer().write(((BytesEvent) lastEvent).getBuffer().getAll(),rbSize);
                remainingBytes.getBuffer().setPosition(0);
            }else{
                return lastEvent;
            }
        }
        BaseEvent currentEvent = remainingBytes;
        if (lastRemainingBytesSize == remainingBytes.getBuffer().size()
                && remainingBytes.getBuffer().size() > 0
        ) {
            unchangedDataTest--;
            if (unchangedDataTest <= 0) {
                var size = Math.min(remainingBytes.getBuffer().size(), 20);

                var content = BBuffer.toHexByteArray(remainingBytes.getBuffer().getBytes(size));
                log.error("[SERVER][RX] Missing handler for buffer: " + content);
                throw new UnknownHandlerException("Missing handler for buffer: "+content);
            }
        } else {
            unchangedDataTest = MAX_REMINDER_TESTS;
            lastRemainingBytesSize = remainingBytes.getBuffer().size();
        }

        return currentEvent;
    }

    @Override
    public void runException(Exception ex) {
        resultBuffer = buildBuffer((NetworkProtoDescriptor) descriptor);
        super.runException(ex);
        try {
            client.close();
        } catch (IOException e) {

        }
    }

    @Override
    protected List<ReturnMessage> runExceptionInternal(Exception ex, ProtoState state, BaseEvent event) {
        if (event instanceof BytesEvent) {
            var rb = ((BytesEvent) event).getBuffer();
            var size = Math.min(rb.size(), 20);

            var content = BBuffer.toHexByteArray(rb.getBytes(size));
            log.error("Exception buffer ("+rb.getAll().length+"):\n" + content);
        }
        return new ArrayList<>();
    }

    @Override
    protected void postExecute() {
        if (remainingBytes.getBuffer().size() >= remainingBytes.getBuffer().getPosition()) {
            remainingBytes.getBuffer().truncate();
        }
        resultBuffer = buildBuffer((NetworkProtoDescriptor) descriptor);
    }

    public void runGreetings() {
        this.send(new BytesEvent(this, NullState.class, buildBuffer()));
    }

    public BBuffer buildBuffer() {
        return buildBuffer((NetworkProtoDescriptor) descriptor);
    }

    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new BBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }

    @Override
    protected void postStop(ProtoState executor) {
        try {
            client.close();
        } catch (IOException e) {
            log.warn("Closed connection: " + executor.getClass().getSimpleName());
        }
        super.postStop(executor);
    }

    private void senderThread() {
        while (true) {
            var networkStep = outputQueue.poll();
            if (networkStep != null) {
                super.runSteps(networkStep.step, networkStep.runner);
            } else {
                Sleeper.yield(1);
            }
        }
    }


    @Override
    public void runSteps(Iterator<ProtoStep> stepsToInvoke, ProtoState executor) {
        outputQueue.add(new NetworkStep(stepsToInvoke, executor));
    }

    private static class NetworkStep {
        public Iterator<ProtoStep> step;
        public ProtoState runner;

        public NetworkStep(Iterator<ProtoStep> step, ProtoState runner) {
            this.step = step;
            this.runner = runner;
        }
    }
}
