package org.kendar.protocol.context;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.exceptions.ConnectionExeception;
import org.kendar.exceptions.UnknownCommandException;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.NullState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.Stop;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.proxy.Proxy;
import org.kendar.tcpserver.ClientServerChannel;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Instance of a network protocol defintion
 */
public abstract class NetworkProtoContext extends ProtoContext {
    private static final Logger log = LoggerFactory.getLogger(NetworkProtoContext.class);
    private final List<Runnable> runnables = new ArrayList<>();
    /**
     * If had sent the greeting message (to send data immediatly after connection without further ado)
     */
    private boolean greetingsSent = false;
    /**
     * Wrapper for the connection with the client
     */
    private ClientServerChannel client;
    /**
     * Proxy to call the real server
     */
    private Proxy proxy;
    /**
     * Storage for the bytes not consumed
     */
    private BytesEvent remainingBytes;
    private boolean disconnected = false;

    public NetworkProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    /**
     * If no data continue, else missing handler!
     *
     * @param event
     * @return
     */
    private static ProtoState continueOrThrowMissingHandler(ProtocolEvent event) {
        var message = "Missing event handler for " + event.getClass().getSimpleName();
        if (event instanceof BytesEvent) {
            var remainingBytes = (BytesEvent) event;
            if (remainingBytes.getBuffer().size() == 0) {
                return null;
            }
            message = event + " " + remainingBytes.getBuffer().toHexStringUpToLength(20);
        } else {
            message = event.toString();
        }
        if (!event.getTag().isEmpty()) {
            message += " tags:" + event.getTagKeyValues();
        }
        log.error("[SERVER][??] Unknown: {}", message);
        throw new UnknownCommandException("Unknown command issued: " + message);
    }

    public List<Runnable> getRunnables() {
        try {
            return new ArrayList<>(runnables);
        } finally {
            runnables.clear();
        }
    }

    @Override
    public void disconnect(Object connection) {
        try {
            disconnected = true;
            if (connection != null && NetworkProxySocket.class.isAssignableFrom(connection.getClass())) {
                ((NetworkProxySocket) connection).close();
            }
            if (client != null) client.close();
        } catch (IOException e) {

        }
    }

    /**
     * Write to the client socket, calls the method to serialize the message
     *
     * @param rm
     */
    @Override
    public void write(ReturnMessage rm) {
        if (rm instanceof Stop) return;
        if (disconnected) {
            if (client != null && client.isOpen()) {
                try {
                    client.close();
                    client = null;
                } catch (IOException e) {

                }
            }
        }
        updateLastAccess();
        var returnMessage = (NetworkReturnMessage) rm;
        //Create a new buffer fit for the destination
        var resultBuffer = buildBuffer();
        //Write on the buffer the message content
        returnMessage.write(resultBuffer);
        var length = resultBuffer.size();
        //Create a bytebuffer fitting
        var response = ByteBuffer.allocate(length);
        response.put(resultBuffer.toArray());
        //To send
        response.flip();
        log.debug("[CL<TP][TX]: Sending back: {}", returnMessage.getClass().getSimpleName());
        var res = client.write(response);
        if (res != null) {
            try {
                res.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("[CL<TP][TX] Cannot write message: {} {}", returnMessage.getClass().getSimpleName(), e.getMessage());
                throw new ConnectionExeception("Cannot write on channel");
            }
        }


    }

    @Override
    protected void postWrite(ReturnMessage stepResult) {
        if (disconnected) return;
        var toRun = getRunnables();
        if (toRun.isEmpty()) return;
        executorService.submit(() -> {
            for (var runnable : toRun) {
                runnable.run();
            }
            updateLastAccess();
        });
    }

    /**
     * Internal, used only by the network protocol descriptor
     *
     * @param client
     */
    public void setClient(ClientServerChannel client) {
        if (this.client == null) {
            this.client = client;
        } else {
            log.error("[SERVER] Cannot reassign channel");
            throw new ConnectionExeception("Cannot reassign channel");
        }
    }

    /**
     * Retrieve the current proxy instance
     *
     * @return
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Set the current proxy instance
     *
     * @param proxy
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    protected ProtoState findThePossibleNextStateOnStack(ProtocolEvent event, int maxDepth) {
        var eventTags = event.getTagKeyValues();
        if (executionStack.get(eventTags).empty()) {
            //If no remaining bytes or no handeler throw
            return continueOrThrowMissingHandler(event);
        }
        //Send greetings if needed
        if (event instanceof BytesEvent) {
            if (((BytesEvent) event).getBuffer().size() == 0) {
                if (greetingsSent || !((NetworkProtoDescriptor) descriptor).sendImmediateGreeting()) {
                    try {
                        return null;
                    } finally {
                        Sleeper.yield();
                    }

                } else if (!greetingsSent) {
                    greetingsSent = true;
                }
            }
        }
        return super.findThePossibleNextStateOnStack(event, maxDepth);
    }

    /**
     * in case of exception should close the client connection
     *
     * @param ex
     */
    @Override
    public void handleExceptionInternal(Exception ex) {
        super.handleExceptionInternal(ex);
        try {
            client.close();
        } catch (IOException e) {
            log.trace("Ignorable", e);
        }
    }

    /**
     * Default implementation simply do nothing but printing what's missing
     *
     * @param ex
     * @param state
     * @param event
     * @return
     */
    @Override
    protected List<ReturnMessage> runException(Exception ex, ProtoState state, ProtocolEvent event) {
        if (event instanceof BytesEvent) {
            var rb = ((BytesEvent) event).getBuffer();
            log.error("Exception buffer ({}):\n{}", rb.getAll().length, rb.toHexStringUpToLength(20));
        }
        return new ArrayList<>();
    }

    /**
     * Repost the remaining data if something left
     *
     * @param currentEvent
     */
    @Override
    protected void postExecute(ProtocolEvent currentEvent) {
        if (currentEvent instanceof BytesEvent) {
            var remainingBytes = (BytesEvent) currentEvent;
            if (remainingBytes.getBuffer().size() >= remainingBytes.getBuffer().getPosition()) {
                remainingBytes.getBuffer().truncate();
                sendSync(remainingBytes);
            }
        }
    }

    /**
     * Add new bytes to the currently remaining bytes. In case of "askmoredata"
     * just continue without doing nothing
     *
     * @param currentEvent
     * @return
     */
    @Override
    public boolean reactToEvent(ProtocolEvent currentEvent) {
        try {
            updateLastAccess();
            if (currentEvent instanceof BytesEvent) {
                var be = (BytesEvent) currentEvent;
                if (remainingBytes != null && remainingBytes.getBuffer().size() > 0) {
                    log.trace("[SERVER][RX] Adding to remaining bytes");
                    remainingBytes.getBuffer().setPosition(remainingBytes.getBuffer().size());
                    remainingBytes.getBuffer().write(be.getBuffer().getAll());
                    remainingBytes.getBuffer().setPosition(0);
                    currentEvent = remainingBytes;
                    remainingBytes = null;

                }
            }
            return super.reactToEvent(currentEvent);
        } catch (AskMoreDataException ex) {
            log.trace("[SERVER][RX] Asking for more data");
            remainingBytes = (BytesEvent) currentEvent;
            return true;
        }
    }

    /**
     * Send the greetings to the server
     */
    public void sendGreetings() {
        updateLastAccess();
        this.send(new BytesEvent(this, NullState.class, buildBuffer()));
    }

    /**
     * Create a buffer (BigEndian/LittleEndian/BlahBlahBlah) according to
     * the protocol descriptor
     *
     * @return
     */
    public BBuffer buildBuffer() {
        return buildBuffer((NetworkProtoDescriptor) descriptor);
    }

    /**
     * As before but discerning exactly what is wanted
     *
     * @param descriptor
     * @return
     */
    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new BBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }

    /**
     * After the stop
     *
     * @param executor
     */
    @Override
    protected void postStop(ProtoState executor) {
        try {
            client.close();
        } catch (IOException e) {
            log.warn("[CL>TP] Closed connection: {}", executor.getClass().getSimpleName());
        }
        super.postStop(executor);
    }

    /**
     * Run steps through the executor
     *
     * @param stepsToInvoke
     * @param executor
     * @param event
     */
    @Override
    public void runSteps(Iterator<ProtoStep> stepsToInvoke, ProtoState executor, ProtocolEvent event) {
        updateLastAccess();
        executorService.execute(() -> {
            updateLastAccess();
            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", contextId + "")) {
                super.runSteps(stepsToInvoke, executor, event);
            }
        });
    }

    public void setActive() {
        updateLastAccess();
    }

    public void addResponse(Runnable toRun) {
        runnables.add(toRun);
    }
}
