package com.talhanation.recruits.pathfinding;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
    private static final int workersCount = RecruitsServerConfig.AsyncPathfindingThreadsCount.get();
    private static final Executor pathFindingExecutor = new ThreadPoolExecutor(
            1,
            workersCount,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder()
                    .setNameFormat("recruits-path-processor-%d")
                    .setPriority(Thread.NORM_PRIORITY - 2)
                    .build()
    );
    protected static void queue(@NotNull AsyncPath path) {
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