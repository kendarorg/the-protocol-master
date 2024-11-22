package org.kendar.utils;

import java.util.Timer;
import java.util.TimerTask;

public class TimerService {
    private static final Timer timer = new Timer();

    public TimerService() {

    }


    public TimerTask scheduleOnce(Runnable task, long delay) {
        var timerTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };
        timer.schedule(timerTask, delay);
        return timerTask;
    }

    public TimerTask schedule(Runnable task, long delay, long period) {
        var timerTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };
        timer.scheduleAtFixedRate(timerTask, delay, period);
        return timerTask;
    }
}
