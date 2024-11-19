package org.kendar.utils;

import java.util.function.BooleanSupplier;

/**
 * No thread lock wait
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class Sleeper {
    /**
     * Runs a synchronized based wait mechanism instead of sleep
     *
     * @param timeoutMillis Timeout in ms
     */
    @SuppressWarnings("CatchMayIgnoreException")
    public static void sleep(long timeoutMillis) {
        try {
            Object obj = new Object();
            synchronized (obj) {
                obj.wait(timeoutMillis);
            }
        } catch (Exception ex) {

        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    public static void sleep(long timeoutMillis, BooleanSupplier booleanSupplier) {
        try {
            Object obj = new Object();
            var times = (int) timeoutMillis / 100;
            for (int i = 0; i < 100; i++) {
                synchronized (obj) {
                    obj.wait(times);
                }
                if (booleanSupplier.getAsBoolean()) {
                    return;
                }
            }

        } catch (Exception ex) {

        }
        throw new RuntimeException("Sleeper sleep timed out");
    }

    public static void sleepNoException(long timeoutMillis, BooleanSupplier booleanSupplier) {
        sleepNoException(timeoutMillis, booleanSupplier, false);
    }

    public static void sleepNoException(long timeoutMillis, BooleanSupplier booleanSupplier, boolean silent) {
        try {
            Object obj = new Object();
            var times = (int) timeoutMillis / 100;
            for (int i = 0; i < 100; i++) {
                synchronized (obj) {
                    obj.wait(times);
                }
                if (booleanSupplier.getAsBoolean()) {
                    return;
                }
            }

        } catch (Exception ex) {

        }
        if (!silent) {
            System.out.println("Sleeper sleep timed out with no answer");
        }
    }

    /**
     * Give control to other threads
     */
    public static void yield() {
        Thread.yield();
    }
}
