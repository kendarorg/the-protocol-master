package org.kendar.server.config;

import java.security.PrivilegedAction;

public class ServerConfig {

    private static final int DEFAULT_IDLE_TIMER_SCHEDULE_MILLIS = 10000; // 10 sec.

    private static final long DEFAULT_IDLE_INTERVAL_IN_SECS = 30;
    private static final int DEFAULT_MAX_CONNECTIONS = -1; // no limit on maximum connections
    private static final int DEFAULT_MAX_IDLE_CONNECTIONS = 200;

    private static final long DEFAULT_MAX_REQ_TIME = -1; // default: forever
    private static final long DEFAULT_MAX_RSP_TIME = -1; // default: forever
    // default timer schedule, in milli seconds, for the timer task that's responsible for
    // timing out request/response if max request/response time is configured
    private static final long DEFAULT_REQ_RSP_TIMER_TASK_SCHEDULE_MILLIS = 1000;
    private static final int DEFAULT_MAX_REQ_HEADERS = 200;
    private static final long DEFAULT_DRAIN_AMOUNT = 64 * 1024;

    private static long idleTimerScheduleMillis;
    private static long idleIntervalMillis;
    // The maximum number of bytes to drain from an inputstream
    private static long drainAmount;
    // the maximum number of connections that the server will allow to be open
    // after which it will no longer "accept()" any new connections, till the
    // current connection count goes down due to completion of processing the requests
    private static int maxConnections;
    private static int maxIdleConnections;
    // The maximum number of request headers allowable
    private static int maxReqHeaders;
    // max time a request or response is allowed to take
    private static long maxReqTime;
    private static long maxRspTime;
    private static long reqRspTimerScheduleMillis;
    private static boolean debug;

    // the value of the TCP_NODELAY socket-level option
    private static boolean noDelay;

    static {
        java.security.AccessController.doPrivileged(
                new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        idleIntervalMillis = Long.getLong("sun.net.httpserver.idleInterval",
                                DEFAULT_IDLE_INTERVAL_IN_SECS) * 1000;
                        if (idleIntervalMillis <= 0) {
                            idleIntervalMillis = DEFAULT_IDLE_INTERVAL_IN_SECS * 1000;
                        }

                        idleTimerScheduleMillis = Long.getLong("sun.net.httpserver.clockTick",
                                DEFAULT_IDLE_TIMER_SCHEDULE_MILLIS);
                        if (idleTimerScheduleMillis <= 0) {
                            // ignore zero or negative value and use the default schedule
                            idleTimerScheduleMillis = DEFAULT_IDLE_TIMER_SCHEDULE_MILLIS;
                        }

                        maxConnections = Integer.getInteger(
                                "jdk.httpserver.maxConnections",
                                DEFAULT_MAX_CONNECTIONS);

                        maxIdleConnections = Integer.getInteger(
                                "sun.net.httpserver.maxIdleConnections",
                                DEFAULT_MAX_IDLE_CONNECTIONS);

                        drainAmount = Long.getLong("sun.net.httpserver.drainAmount",
                                DEFAULT_DRAIN_AMOUNT);

                        maxReqHeaders = Integer.getInteger(
                                "sun.net.httpserver.maxReqHeaders",
                                DEFAULT_MAX_REQ_HEADERS);

                        maxReqTime = Long.getLong("sun.net.httpserver.maxReqTime",
                                DEFAULT_MAX_REQ_TIME);

                        maxRspTime = Long.getLong("sun.net.httpserver.maxRspTime",
                                DEFAULT_MAX_RSP_TIME);

                        reqRspTimerScheduleMillis = Long.getLong("sun.net.httpserver.timerMillis",
                                DEFAULT_REQ_RSP_TIMER_TASK_SCHEDULE_MILLIS);
                        if (reqRspTimerScheduleMillis <= 0) {
                            // ignore any negative or zero value for this configuration and reset
                            // to default schedule
                            reqRspTimerScheduleMillis = DEFAULT_REQ_RSP_TIMER_TASK_SCHEDULE_MILLIS;
                        }

                        debug = Boolean.getBoolean("sun.net.httpserver.debug");

                        noDelay = Boolean.getBoolean("sun.net.httpserver.nodelay");

                        return null;
                    }
                });

    }

    public static void checkLegacyProperties(final System.Logger log) {

        // legacy properties that are no longer used
        // print a warning to log if they are set.

        java.security.AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> {
                    if (System.getProperty("sun.net.httpserver.readTimeout")
                            != null) {
                        log.log(System.Logger.Level.WARNING,
                                "sun.net.httpserver.readTimeout " +
                                        "property is no longer used. " +
                                        "Use sun.net.httpserver.maxReqTime instead."
                        );
                    }
                    if (System.getProperty("sun.net.httpserver.writeTimeout")
                            != null) {
                        log.log(System.Logger.Level.WARNING,
                                "sun.net.httpserver.writeTimeout " +
                                        "property is no longer used. Use " +
                                        "sun.net.httpserver.maxRspTime instead."
                        );
                    }
                    if (System.getProperty("sun.net.httpserver.selCacheTimeout")
                            != null) {
                        log.log(System.Logger.Level.WARNING,
                                "sun.net.httpserver.selCacheTimeout " +
                                        "property is no longer used."
                        );
                    }
                    return null;
                }
        );
    }

    public static boolean debugEnabled() {
        return debug;
    }

    /**
     * {@return Returns the maximum duration, in milli seconds, a connection can be idle}
     */
    public static long getIdleIntervalMillis() {
        return idleIntervalMillis;
    }

    /**
     * {@return Returns the schedule, in milli seconds, for the timer task that is responsible
     * for managing the idle connections}
     */
    public static long getIdleTimerScheduleMillis() {
        return idleTimerScheduleMillis;
    }

    /**
     * @return Returns the maximum number of connections that can be open at any given time.
     * This method can return a value of 0 or negative to represent that the limit hasn't
     * been configured.
     */
    public static int getMaxConnections() {
        return maxConnections;
    }

    /**
     * @return Returns the maximum number of connections that can be idle. This method
     * can return a value of 0 or negative.
     */
    public static int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public static long getDrainAmount() {
        return drainAmount;
    }

    public static int getMaxReqHeaders() {
        return maxReqHeaders;
    }

    /**
     * @return Returns the maximum amount of time the server will wait for the request to be read
     * completely. This method can return a value of 0 or negative to imply no maximum limit has
     * been configured.
     */
    public static long getMaxReqTime() {
        return maxReqTime;
    }

    /**
     * @return Returns the maximum amount of time the server will wait for the response to be generated
     * for a request that is being processed. This method can return a value of 0 or negative to
     * imply no maximum limit has been configured.
     */
    public static long getMaxRspTime() {
        return maxRspTime;
    }

    /**
     * {@return Returns the timer schedule of the task that's responsible for timing out
     * request/response that have been running longer than any configured timeout}
     */
    public static long getReqRspTimerScheduleMillis() {
        return reqRspTimerScheduleMillis;
    }

    public static boolean noDelay() {
        return noDelay;
    }
}
