package com.talhanation.recruits.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class CommonThreadExecutor {
    private static final int workersCount = Math.max(Runtime.getRuntime().availableProcessors() / 4, 1);
    private static final Executor commonExecutor = new ThreadPoolExecutor(
            workersCount,
            workersCount,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder()
                    .setNameFormat("petal-path-processor-%d")
                    .setPriority(Thread.NORM_PRIORITY - 2)
                    .build()
    );

    public static CompletableFuture<Void> queue(@NotNull Runnable runnable) {
        return CompletableFuture.runAsync(runnable, commonExecutor);
    }

    public static int getWorkersCount() {
        return workersCount;
    }
}
