package com.talhanation.recruits.pathfinding;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * used to handle the scheduling of async path processing
 */
public class AsyncPathProcessor {
    private static final int workersCount = Math.max(1, RecruitsServerConfig.AsyncPathfindingThreadsCount.get());

    private static final int QUEUE_CAPACITY = workersCount * 8;

    private static final ThreadPoolExecutor pathFindingExecutor = new ThreadPoolExecutor(
            1,
            workersCount,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactoryBuilder()
                    .setNameFormat("recruits-path-processor-%d")
                    .setDaemon(true) // Fix: Daemon-Threads damit der Server sauber beendet werden kann
                    .setPriority(Thread.NORM_PRIORITY - 2)
                    .build(),
            (task, executor) -> {
                if (!executor.isShutdown()) {
                    Main.LOGGER.debug("AsyncPathProcessor queue full (capacity {}), applying backpressure", QUEUE_CAPACITY);
                    task.run();
                }
            }
    );

    public static void shutdown() {
        pathFindingExecutor.shutdown();
        try {
            if (!pathFindingExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                pathFindingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            pathFindingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            Main.LOGGER.warn("AsyncPathProcessor shutdown interrupted");
        }
    }

    protected static void queue(@NotNull AsyncPath path) {
        if (pathFindingExecutor.isShutdown()) return;
        CompletableFuture.runAsync(path::process, pathFindingExecutor);
    }

    /**
     * takes a possibly unprocessed path, and waits until it is completed
     * the consumer will be immediately invoked if the path is already processed
     * the consumer will always be called on the main thread
     *
     * @param path            a path to wait on
     * @param afterProcessing a consumer to be called
     */
    public static void awaitProcessing(@Nullable Path path, MinecraftServer server, Consumer<@Nullable Path> afterProcessing) {
        if (path instanceof AsyncPath asyncPath && !asyncPath.isProcessed()) {
            asyncPath.postProcessing(() -> server.execute(() -> afterProcessing.accept(path)));
        } else {
            afterProcessing.accept(path);
        }
    }
}