package com.talhanation.recruits.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DelayedExecutor {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = Executors.defaultThreadFactory().newThread(task);
        thread.setDaemon(true);
        return thread;
    });

    public static void runLater(Runnable task, long delayMillis) {
        scheduler.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }
}
