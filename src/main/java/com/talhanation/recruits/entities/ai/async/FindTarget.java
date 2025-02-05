package com.talhanation.recruits.entities.ai.async;

import com.talhanation.recruits.util.ProcessState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;


public class FindTarget<T extends LivingEntity>  {
    public volatile ProcessState processState = ProcessState.WAITING;
    private final double followDistance;
    private final List<Runnable> postProcessing = new ArrayList<>(0);

    protected final Class<T> targetType;

    private Deque<LivingEntity> targetStack = new ArrayDeque<>();

    protected TargetingConditions targetConditionsNormal;
    private final Mob mob;

    public FindTarget(Mob mob, Class<T> targetType, double followDistance, @Nullable Predicate<LivingEntity> predicate) {
        this.targetType = targetType;
        this.mob = mob;
        this.followDistance = followDistance;
        this.targetConditionsNormal = TargetingConditions.forCombat().range(followDistance).selector(predicate).ignoreLineOfSight();
    }

    protected AABB getTargetSearchArea(double range) {
        return this.mob.getBoundingBox().inflate(range, range, range);
    }

    public synchronized void findTargetNormal() {
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) {
            return;
        }

        if (this.processState == ProcessState.COMPLETED || this.processState == ProcessState.PROCESSING) {
            return;
        }

        processState = ProcessState.PROCESSING;

        List<T> list = this.mob.getCommandSenderWorld().getEntitiesOfClass(
                this.targetType,
                this.getTargetSearchArea(this.followDistance)
        );
        list.sort(Comparator.comparingDouble(entry -> entry.distanceToSqr(this.mob)));

        Deque<LivingEntity> newStack = new ArrayDeque<>();

        for (T entry : list) {
            boolean passesFilter = this.targetConditionsNormal.test(this.mob, entry);

            if (!passesFilter) continue;

            newStack.addLast(entry);
        }

        if (newStack.isEmpty()) {
            this.processState = ProcessState.COMPLETED;
            return;
        }

        this.targetStack = newStack;

        processState = ProcessState.COMPLETED;

        for (Runnable runnable : this.postProcessing) {
            runnable.run();
        }
    }

    public synchronized void postProcessing(@NotNull Runnable runnable) {
        if (isProcessed()) {
            runnable.run();
        } else {
            this.postProcessing.add(runnable);
        }
    }

    public Deque<LivingEntity> getTargets() {
        this.checkProcessed();

        return this.targetStack;
    }

    private void checkProcessed() {
        if (this.processState == ProcessState.WAITING || this.processState == ProcessState.PROCESSING) {
            this.findTargetNormal();
        }
    }

    public boolean isProcessed() {
        return this.processState == ProcessState.COMPLETED;
    }


    public synchronized void reset() {
        this.processState = ProcessState.WAITING;
        this.targetStack.clear();
        this.postProcessing.clear();
    }

}
