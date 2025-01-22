package com.talhanation.recruits.pathfinding;

import com.talhanation.recruits.util.CommonThreadExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * used to handle the scheduling of async path processing
 */
public class AsyncPathProcessor {
    protected static void queue(@NotNull AsyncPath path) {
        CommonThreadExecutor.queue(path::process);
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