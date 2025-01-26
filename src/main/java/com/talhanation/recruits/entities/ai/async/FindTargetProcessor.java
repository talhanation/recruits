package com.talhanation.recruits.entities.ai.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class FindTargetProcessor {
    private static final int workersCount = RecruitsServerConfig.AsyncTargetFindingThreadsCount.get();
    private static final Executor commonExecutor = new ThreadPoolExecutor(
            1,
            workersCount,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder()
                    .setNameFormat("recruits-target-find-processor-%d")
                    .setPriority(Thread.NORM_PRIORITY - 2)
                    .build()
    );

    public static <T extends LivingEntity> void queue(@NotNull FindTarget<T> target) {
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return;
        CompletableFuture.runAsync(target::findTargetNormal, commonExecutor);
    }

    public static <T extends LivingEntity> void awaitProcessing(@Nullable FindTarget<T> findTarget, MinecraftServer server, Consumer<@Nullable FindTarget<T>> afterProcessing) {
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return;
        if (findTarget == null) {
            afterProcessing.accept(null);
            return;
        }

        if (!findTarget.isProcessed()) {
            findTarget.postProcessing(() -> server.execute(() -> afterProcessing.accept(findTarget)));
        } else {
            afterProcessing.accept(findTarget);
        }
    }
}
