package org.kendar.tests.utils;


import java.util.function.BooleanSupplier;

/**
 * No thread lock wait
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class TestSleeper {
    /**
     * Runs a synchronized based wait mechanism instead of sleep
     *
     * @param timeoutMillis Timeout in ms
     */
    @SuppressWarnings("CatchMayIgnoreException")
    public static void sleep(long timeoutMillis) {

        try {
            if (timeoutMillis == 0) {
                Thread.onSpinWait();
                return;
            }
            Object obj = new Object();
            synchronized (obj) {
                obj.wait(timeoutMillis);
            }
        } catch (Exception ex) {

        }
    }

    public static void sleep(long timeoutMillis, BooleanSupplier booleanSupplier) {
        sleep(timeoutMillis,booleanSupplier,"Timeout reached");
    }
        @SuppressWarnings("CatchMayIgnoreException")
    public static void sleep(long timeoutMillis, BooleanSupplier booleanSupplier,String errorMessage) {
        try {
            Object obj = new Object();

            var times = (int) timeoutMillis;
            var counter = 100;
            if(times<=100)counter =2;
            for (int i = 0; i < timeoutMillis; i+=counter) {
                synchronized (obj) {
                    obj.wait(counter);
                }
                if (booleanSupplier.getAsBoolean()) {
                    return;
                }
            }

        } catch (Exception ex) {

        }
        throw new RuntimeException(errorMessage);
    }

    public static boolean sleepNoException(long timeoutMillis, BooleanSupplier booleanSupplier) {
        return sleepNoException(timeoutMillis, booleanSupplier, false);
    }

    public static boolean sleepNoException(long timeoutMillis, BooleanSupplier booleanSupplier, boolean silent) {
        try {
            Object obj = new Object();
            var times = (int) timeoutMillis / 100;
            for (int i = 0; i < 100; i++) {
                synchronized (obj) {
                    obj.wait(times);
                }
                if (booleanSupplier.getAsBoolean()) {
                    return true;
                }
            }

        } catch (Exception ex) {

        }
        if (!silent) {
            System.out.println("Sleeper sleep timed out with no answer");
        }
        return false;
    }

    /**
     * Give control to other threads
     */
    public static void yield() {
        Thread.onSpinWait();
        //Sleeper.sleep(1);
    }
}