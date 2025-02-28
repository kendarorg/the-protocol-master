package org.kendar.utils;

import java.util.Random;

public class ChaosUtils {
    public static void randomWait(int min,int max){
        Random r = new Random();
        int waitMs = r.nextInt(max - min) + min;
        if (waitMs > 0) {
            Sleeper.sleep(waitMs);
        }
    }

    public static boolean randomAction(int requiredPercent) {
        var pc = ((double) requiredPercent) / 100.0;
        return Math.random() < pc;
    }
}
