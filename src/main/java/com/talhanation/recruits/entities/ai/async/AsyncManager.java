package com.talhanation.recruits.entities.ai.async;

import com.talhanation.recruits.config.RecruitsServerConfig;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncManager {
    public static final int THREADS = RecruitsServerConfig.AsyncTargetFindingThreadsCount.get();
    private static final AtomicInteger threadCount = new AtomicInteger(1);

    private static final ThreadFactory threadFactory = runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Recruits-Target-Async-" + threadCount.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    };

    public static final Executor executor = Executors.newFixedThreadPool(THREADS, threadFactory);
}
