package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.async.FindTarget;
import com.talhanation.recruits.entities.ai.async.FindTargetProcessor;
import com.talhanation.recruits.util.ProcessState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;public class RecruitNearestAttackableTargetGoal<T extends LivingEntity> extends Goal {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Predicate<LivingEntity> predicate;
    protected final Class<T> targetType;
    private final AbstractRecruitEntity recruit;
    protected Deque<LivingEntity> targets = new ArrayDeque<>();
    private long lastCanUseCheck;
    private FindTarget<T> findTarget;

    public RecruitNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> targetType, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> predicate) {
        this.recruit = recruit;
        this.targetType = targetType;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.predicate = predicate;
    }

    @Override
    public boolean canUse() {
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) {
            return false;
        }

        long i = this.recruit.level.getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !targets.isEmpty();
    }

    @Override
    public void start() {
        if (this.findTarget == null) {
            this.findTarget = new FindTarget<>(this.recruit, this.targetType, getFollowDistance(), this.predicate);
        } else {
            this.findTarget.reset();
        }

        if (RecruitsServerConfig.UseAsyncTargetFinding.get()) {
            scheduleFindTargets();
        } else {
            findNewTargets();
        }
    }


    @Override
    public void tick() {
        super.tick();
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) {
            return;
        }

        LivingEntity entity = recruit.getTarget();

        if (!targets.isEmpty() && (entity == null || !entity.isAlive())) {
            selectNextTarget();
        }
    }

    private void findNewTargets() {
        findTarget.findTargetNormal();

        if (findTarget.getTargets() == null) return;

        this.targets = findTarget.getTargets();
    }

    private void selectNextTarget() {
        while (!targets.isEmpty()) {
            LivingEntity target = targets.pop();
            if (target == null) return;

            boolean seeing = this.recruit.hasLineOfSight(target);
            boolean isInFight = isInFight(target);
            boolean alive = target.isAlive();

            if (alive && seeing && !isInFight) {
                this.recruit.setTarget(target);
                return;
            }
        }
    }

    private boolean canReach(LivingEntity p_26149_) {
        Path path = this.recruit.getNavigation().createPath(p_26149_, 0);
        if (path == null) {
            return false;
        } else {
            Node node = path.getEndNode();
            if (node == null) {
                return false;
            } else {
                int i = node.x - p_26149_.getBlockX();
                int j = node.z - p_26149_.getBlockZ();
                return (double)(i * i + j * j) <= 2.25D;
            }
        }
    }

    private boolean isInFight(LivingEntity living) {
        if (living != null) {
            int targetLastHurt = recruit.tickCount - living.getLastHurtByMobTimestamp();
            LivingEntity targetHurtEntity = living.getLastHurtByMob();

            if (targetHurtEntity != null && !targetHurtEntity.equals(this.recruit)) {
                return targetLastHurt < 10;
            }
        }
        return false;
    }

    protected double getFollowDistance() {
        return this.recruit.getAttributeValue(Attributes.FOLLOW_RANGE);
    }


    private synchronized void scheduleFindTargets() {
        lock.readLock().lock();
        try {
            if (findTarget.processState == ProcessState.PROCESSING) return;
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            FindTargetProcessor.queue(this.findTarget, this.recruit.getCommandSenderWorld());

            FindTargetProcessor.awaitProcessing(this.findTarget, this.recruit.getCommandSenderWorld(), (FindTarget<T> processedFindTarget) -> {
                if (processedFindTarget == null || processedFindTarget != this.findTarget) return;
                this.targets = processedFindTarget.getTargets();
            });

        } finally {
            lock.writeLock().unlock();
        }
    }


}

