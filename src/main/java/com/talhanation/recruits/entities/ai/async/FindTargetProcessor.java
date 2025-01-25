package com.talhanation.recruits.entities.ai.async;

import com.talhanation.recruits.pathfinding.AsyncPath;
import com.talhanation.recruits.util.CommonThreadExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FindTargetProcessor {
    public static <T extends LivingEntity> void queue(@NotNull FindTarget<T> target) {
        CommonThreadExecutor.queue(target::findTargetNormal);
    }

    public static <T extends LivingEntity> void awaitProcessing(@Nullable FindTarget<T> findTarget, MinecraftServer server, Consumer<@Nullable FindTarget<T>> afterProcessing) {
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
