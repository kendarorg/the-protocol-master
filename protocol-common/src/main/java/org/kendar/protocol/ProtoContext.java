package org.kendar.protocol;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.protocol.fsm.BaseEvent;
import org.kendar.protocol.fsm.NullState;
import org.kendar.protocol.fsm.Start;
import org.kendar.protocol.fsm.Stop;
import org.kendar.proxy.Proxy;
import org.kendar.server.Channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;


public class ProtoContext {
    private final Map<String, Object> values = new HashMap<>();


    private final List<Object> queue = new ArrayList<>();
    private final BBuffer inputBuffer;
    private final ProtoDescriptor descriptor;
    private final Channel client;
    private final int MAX_REMINDER_TESTS = 20;
    protected boolean somethingDone = false;
    protected ConcurrentLinkedDeque<BaseEvent> eventsQueue = new ConcurrentLinkedDeque<>();
    private Class<?> prevState;
    private Proxy proxy;
    private boolean transaction;
    private Class<?> currentState;
    private List<BaseEvent> orderedEvents = new ArrayList<>();
    private BytesEvent reminderEvent = null;
    private int reminderTests = MAX_REMINDER_TESTS;
    private int lastReminderSize = -1;


    public ProtoContext(ProtoDescriptor descriptor, Channel client) {
        this.descriptor = descriptor;
        this.client = client;
        this.currentState = Start.class;
        this.prevState = NullState.class;
        this.inputBuffer = buildBuffer(descriptor);
    }

    public List<Object> getQueue() {
        return queue;
    }

    public void putStack(Object ob) {
        queue.add(ob);
    }

    public BBuffer buildBuffer() {
        return buildBuffer(descriptor);
    }

    protected BBuffer buildBuffer(ProtoDescriptor descriptor) {
        return new BBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }

    public void runGreetings() {
        this.send(new BytesEvent(this, NullState.class, buildBuffer()));
    }

    public void runException(Exception ex) {
        var exceptionResults = runExceptionInternal(ex);
        var resultBuffer = buildBuffer(descriptor);
        for (var exceptionResult : exceptionResults) {
            System.out.println("[SERVER] Message: " + exceptionResult.getClass().getSimpleName());
            write(exceptionResult, resultBuffer);
        }
        try {
            client.close();
        } catch (IOException e) {

        }
    }

    protected List<ReturnMessage> runExceptionInternal(Exception ex) {
        throw new RuntimeException(ex);
    }

    private void write(ReturnMessage returnMessage, BBuffer resultBuffer) {
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
                System.err.println("[SERVER] ERROR Message: " + returnMessage.getClass().getSimpleName() + " " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    public void setValue(String key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    public Object getValue(String key) {
        return values.get(key);
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public boolean isTransaction() {
        return transaction;
    }

    public void setTransaction(boolean transaction) {
        this.transaction = transaction;
    }

    public void send(BaseEvent event) {
        eventsQueue.add(event);
    }

    public void start() {
        reminderEvent = new BytesEvent(this, null, buildBuffer());

        BaseEvent currentEvent = null;
        try {
            while (true) {
                while (!eventsQueue.isEmpty()) {
                    var event = eventsQueue.poll();
                    if (event instanceof BytesEvent) {
                        var be = (BytesEvent) event;
                        reminderEvent.getBuffer().append(be.getBuffer());
                        reminderTests = MAX_REMINDER_TESTS;
                    } else {
                        orderedEvents.add(event);
                    }
                }
                if (!orderedEvents.isEmpty()) {
                    reminderTests = MAX_REMINDER_TESTS;
                    currentEvent = orderedEvents.remove(0);
                } else {
                    currentEvent = reminderEvent;
                    if (lastReminderSize == reminderEvent.getBuffer().size()
                            && reminderEvent.getBuffer().size() > 0
                    ) {
                        reminderTests--;
                        if (reminderTests <= 0) {
                            var size = Math.min(reminderEvent.getBuffer().size(), 20);

                            var content = BBuffer.toHexByteArray(reminderEvent.getBuffer().getBytes(size));
                            System.err.println("[SERVER] Unknown: " + content);
                            throw new RuntimeException("Unknown command issued");
                        }
                    } else {
                        reminderTests = MAX_REMINDER_TESTS;
                        lastReminderSize = reminderEvent.getBuffer().size();
                    }
                }

                if (currentEvent == null) {
                    Sleeper.sleep(1);
                    continue;
                }
//            if(!hasMessagesToParseOrToReceive()){
//                return;
//            }

                somethingDone = false;
                for (var executor : descriptor.getPossibleNext(this.currentState)) {
                    if (executor.canHandle(currentEvent.getClass())) {
                        if (executor.canRunEvent(currentEvent)) {
                            somethingDone = true;
                            this.prevState = this.currentState;
                            this.currentState = executor.getClass();
                            System.out.println("[SERVER] State: " + this.currentState.getSimpleName());

                            var resultBuffer = buildBuffer(descriptor);
                            var stepsToInvoke = executor.executeEvent(currentEvent);
                            if (reminderEvent.getBuffer().size() >= reminderEvent.getBuffer().getPosition()) {
                                reminderEvent.getBuffer().truncate();
                            }
                            if (stepsToInvoke != null) {
                                while (stepsToInvoke.hasNext()) {
                                    var steps = stepsToInvoke.next();
                                    if (steps.getClass() == Stop.class) {
                                        try {
                                            client.close();
                                        } catch (IOException e) {
                                            System.out.println("[SERVER] Closed connection: " + this.currentState.getSimpleName());
                                        }
                                        return;
                                    }
                                    var stepResult = steps.run();
                                    System.out.println("[SERVER] Message: " + stepResult.getClass().getSimpleName());
                                    write(stepResult, resultBuffer);
                                }
                            }
                            break;
                        }
                    }
                }
                if (!somethingDone) {
                    Sleeper.sleep(50);
                }
            }
        } catch (RuntimeException ex) {
            runException(ex);
        }
    }
//
//    private boolean hasMessagesToParseOrToReceive() {
//        var shouldProceed = true;
//        if(currentEvent instanceof BytesEvent && reminderEvent!=null){
//            var be = (BytesEvent) eventsQueue.poll();
//            reminderEvent.getBuffer().append(be.getBuffer());
//            currentEvent = reminderEvent;
//        }else if (reminderEvent!=null){
//            currentEvent = reminderEvent;
//        }else{
//            currentEvent = eventsQueue.poll();
//        }
//
//        if(reminderEvent!=null) {
//            if (reminderEvent.getBuffer().size() == lastReminderSize) {
//                reminderTests--;
//                if (reminderTests <= 0) {
//                    var size = Math.min(reminderEvent.getBuffer().size(),20);
//
//                    var content = BBuffer.toHexByteArray(reminderEvent.getBuffer().getBytes(size));
//                    System.err.println("[SERVER] Unknown: "+content);
//                    runException(new RuntimeException("Unknown command issued"));
//                    shouldProceed = false;
//                }
//            }else{
//                lastReminderSize = reminderEvent.getBuffer().size();
//                reminderTests = MAX_REMINDER_TESTS;
//            }
//        }
//        return shouldProceed;
//    }
}
