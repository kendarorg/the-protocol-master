package org.kendar.events;

import org.kendar.exceptions.TPMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

public class EventsQueue {
    private static final Logger log = LoggerFactory.getLogger(EventsQueue.class);
    private static final AtomicLong size = new AtomicLong(0);
    private static EventsQueue instance = new EventsQueue();
    private final HashMap<String, Map<String, Consumer<TpmEvent>>> eventHandlers = new HashMap<>();

    //private final HashMap<String, Class> conversions = new HashMap<>();
    private final HashMap<String, CommandConsumer> commandHandlers = new HashMap<>();
    private final ConcurrentLinkedQueue<TpmEvent> items = new ConcurrentLinkedQueue<>();
    private boolean running = true;

    private EventsQueue() {
        start();
    }

    public static EventsQueue getInstance() {
        return instance;
    }

    public static void send(TpmEvent event) {
        size.incrementAndGet();
        getInstance().items.add(event);
    }

    public static boolean isEmpty() {
        return size.get() == 0;
    }


    public static <T extends TpmEvent> void register(String id, Consumer<T> consumer, Class<T> clazz) {
        var eventName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        //instance.conversions.put(eventName, clazz);
        var realConsumer = new Consumer<TpmEvent>() {
            @Override
            public void accept(TpmEvent event) {
                consumer.accept((T) event);
            }
        };
        if (!instance.eventHandlers.containsKey(eventName)) {
            instance.eventHandlers.put(eventName, new HashMap<>());
        }
        instance.eventHandlers.get(eventName).put(id, realConsumer);
    }


    public static <T extends TpmEvent> void unregister(String id, Class<T> clazz) {
        var eventName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        if (instance.eventHandlers.containsKey(eventName)) {
            instance.eventHandlers.get(eventName).remove(id);
        }
        instance.commandHandlers.remove(eventName);
    }

    public static <T extends TpmEvent> void registerCommand(String id, Function<T, Object> function, Class<T> clazz) {
        var eventName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        //instance.conversions.put(eventName, clazz);
        var realConsumer = new Function<TpmEvent, Object>() {
            @Override
            public Object apply(TpmEvent event) {
                return function.apply((T) event);
            }
        };
        var prevConsumer = instance.commandHandlers.get(eventName);
        if (prevConsumer == null || prevConsumer.id.equalsIgnoreCase(id)) {
            instance.commandHandlers.put(eventName, new CommandConsumer(id, realConsumer));
        } else {
            throw new TPMException("Duplicate event " + eventName);
        }
    }

    /**
     * Start the event queue. Tried with SingleThreadExecutor BUT it uses LinkedBlockingQueue
     * slowing down execution
     */
    private void start() {
        new Thread(() -> {
            while (running) {
                if (items.isEmpty()) {
                    Thread.onSpinWait();
                    continue;
                }
                var item = items.poll();
                while (item != null) {
                    try {
                        handle(item);
                    } catch (Exception e) {
                        log.warn("Trouble handling {}", item.getClass().getSimpleName(), e);
                    }
                    size.decrementAndGet();
                    item = items.poll();
                }
            }
        }).start();

    }

    public void handle(TpmEvent event) {
        var eventName = event.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        log.error(eventName);
        if (!eventHandlers.containsKey(eventName) &&
                !commandHandlers.containsKey(eventName)) return;
        var handlers = eventHandlers.get(eventName);
        var handler = commandHandlers.get(eventName);
        if (handlers != null) {
            for (var subHandler : handlers.entrySet()) {
                try {
                    subHandler.getValue().accept(event);
                } catch (Exception ex) {
                    log.error("Error executing TpmEvent {}", eventName, ex);
                }
            }
        } else if (handler != null) {
            try {
                handler.consumer.apply(event);
            } catch (Exception ex) {
                log.error("Error executing TpmEvent {}", eventName, ex);
            }
        }
    }

    public List<TpmEvent> clean() {
        var result = new ArrayList<>(items);
        items.clear();
        eventHandlers.clear();
        commandHandlers.clear();
        size.set(0L);
        running = false;
        instance = new EventsQueue();
        return result;
    }

    private static class CommandConsumer {
        public final String id;
        public final Function<TpmEvent, Object> consumer;

        public CommandConsumer(String id, Function<TpmEvent, Object> consumer) {
            this.id = id;
            this.consumer = consumer;
        }
    }
}
