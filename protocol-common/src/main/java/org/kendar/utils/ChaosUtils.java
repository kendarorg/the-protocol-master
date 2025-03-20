package org.kendar.utils;

public class ChaosUtils {
    public static void randomWait(int min, int max) {
        int waitMs = randomBetween(min, max);
        if (waitMs > 0) {
            Sleeper.sleep(waitMs);
        }
    }

    public static int randomBetween(int min, int max) {
        return (int) (Math.random() * (max - min)) + min;
    }

    public static boolean randomAction(int requiredPercent) {
        var pc = ((double) requiredPercent) / 100.0;
        return Math.random() < pc;
    }
}
