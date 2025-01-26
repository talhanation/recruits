package com.talhanation.recruits.entities.ai.async;

import com.google.common.collect.Lists;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.util.ProcessState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FindTarget<T extends LivingEntity>  {
    private volatile ProcessState processState = ProcessState.WAITING;
    private final double followDistance;
    private final List<Runnable> postProcessing = new ArrayList<>(0);
    private class TargetWithFightMark {
        T target;
        boolean isInFight;

        private TargetWithFightMark(T target, boolean isInFight) {
            this.target = target;
            this.isInFight = isInFight;
        }

        private T getTarget() {
            return this.target;
        }

        private boolean getInFight () {
            return this.isInFight;
        }
    }
    protected final Class<T> targetType;

    @Nullable
    protected LivingEntity target;
    protected TargetingConditions targetConditionsNormal;
    private final Mob mob;

    public FindTarget(Mob mob, Class<T> targetType, double followDistance, @Nullable Predicate<LivingEntity> predicate) {
        this.targetType = targetType;
        this.mob = mob;
        this.followDistance = followDistance;
        this.targetConditionsNormal = TargetingConditions.forCombat().range(followDistance).selector(predicate);
    }

    protected AABB getTargetSearchArea(double range) {
        return this.mob.getBoundingBox().inflate(range, range, range);
    }

    protected synchronized void findTargetNormal() {
        if (this.processState == ProcessState.COMPLETED || this.processState == ProcessState.PROCESSING) {
            return;
        }

        processState = ProcessState.PROCESSING;

        List<T> list = this.mob.getCommandSenderWorld().getEntitiesOfClass(
                this.targetType,
                this.getTargetSearchArea(this.followDistance)
        );

        List<TargetWithFightMark> testifiedTargets = Lists.newArrayListWithExpectedSize(list.size());

        for(T entry: list) {
            if(!this.targetConditionsNormal.test(this.mob, entry)){
                continue;
            }

            testifiedTargets.add(new TargetWithFightMark(entry, this.isInFight(entry)));
        }

        T target = null;
        T anyTarget = null;
        double d0 = -1.0D;
        double anyD0 = -1.0D;

        for(TargetWithFightMark t1 : testifiedTargets) {
            double d1 = t1.getTarget().distanceToSqr(this.mob.getX(), this.mob.getY(), this.mob.getZ());
            if (anyTarget == null && (d0 == -1.0D || d1 < d0)) {
                anyTarget = t1.getTarget();
                d0 = d1;
            } else if (!t1.getInFight() && (anyD0 == -1.0D || d1 < anyD0)){
                target = t1.getTarget();
                anyD0 = d1;
            }
        }

        T result = null;

        if (target != null) {
            result = target;
        } else if (anyTarget != null) {
            // We didn't find any target who is testified by target conditions and not fighting.
            // Try to find any target that testifies target conditions (does not matter if it fights or not)
            result = anyTarget;
        }
        // We didn't find any target who is testified by target conditions.
        // So no targets buddy

        this.target = result;

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

    public boolean isInFight(LivingEntity living) {
        if (living != null) {
            int targetLastHurt = mob.tickCount - living.getLastHurtByMobTimestamp();
            LivingEntity targetHurtEntity = living.getLastHurtByMob();

            if (targetHurtEntity != null && !targetHurtEntity.equals(this.mob)) {
                return targetLastHurt < 15;
            }
        }
        return false;
    }

    public LivingEntity getTarget() {
        this.checkProcessed();

        return this.target;
    }

    private void checkProcessed() {
        if (this.processState == ProcessState.WAITING || this.processState == ProcessState.PROCESSING) {
            this.findTargetNormal();
        }
    }

    public boolean isProcessed() {
        return this.processState == ProcessState.COMPLETED;
    }
}
