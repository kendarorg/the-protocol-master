package org.kendar.utils;

import java.util.TimerTask;

public class TimerInstance {
    private final TimerTask timerTask;
    private final TimerService timerService;

    public TimerInstance(TimerTask timerTask, TimerService timerService) {

        this.timerTask = timerTask;
        this.timerService = timerService;
    }

    public void cancel(){
        timerTask.cancel();
    }

    public TimerTask getTimerTask() {
        return timerTask;
    }

    public TimerService getTimerService() {
        return timerService;
    }
}
