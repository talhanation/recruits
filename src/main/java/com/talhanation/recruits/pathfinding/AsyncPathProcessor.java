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

public class AsyncPathProcessor {

    private static volatile ThreadPoolExecutor pathFindingExecutor = null;

    public static void start() {
        ThreadPoolExecutor existing = pathFindingExecutor;
        if (existing != null && !existing.isShutdown()) {
            return;
        }

        if (existing != null) {
            existing.shutdownNow();
        }

        int workersCount = Math.max(1, RecruitsServerConfig.AsyncPathfindingThreadsCount.get());
        int queueCapacity = workersCount * 8;

        pathFindingExecutor = new ThreadPoolExecutor(
                1,
                workersCount,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder()
                        .setNameFormat("recruits-path-processor-%d")
                        .setDaemon(true)
                        .setPriority(Thread.NORM_PRIORITY - 2)
                        .build(),
                (task, executor) -> {
                    if (!executor.isShutdown()) {
                        task.run();
                    }
                }
        );
    }

    public static void shutdown() {
        ThreadPoolExecutor executor = pathFindingExecutor;
        if (executor == null || executor.isShutdown()) return;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            Main.LOGGER.warn("AsyncPathProcessor shutdown interrupted");
        }
    }

    protected static void queue(@NotNull AsyncPath path) {
        ThreadPoolExecutor executor = pathFindingExecutor;
        if (executor == null || executor.isShutdown()) {
            path.process();
            return;
        }
        CompletableFuture.runAsync(path::process, executor);
    }
    
    public static void awaitProcessing(@Nullable Path path, MinecraftServer server, Consumer<@Nullable Path> afterProcessing) {
        if (path instanceof AsyncPath asyncPath && !asyncPath.isProcessed()) {
            asyncPath.postProcessing(() -> server.execute(() -> afterProcessing.accept(path)));
        } else {
            afterProcessing.accept(path);
        }
    }
}