package org.kendar.protocol;

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
}
