package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.async.FindTarget;
import com.talhanation.recruits.entities.ai.async.FindTargetProcessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class RecruitNearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
    private static final int DEFAULT_RANDOM_INTERVAL = 10;
    private FindTarget<T> findTarget;
    private final Predicate<LivingEntity> predicate;
    protected final Class<T> targetType;
    protected final int randomInterval;

    @Nullable
    protected LivingEntity target;

    public RecruitNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> predicate) {
        super(recruit, mustSee, mustReach);
        this.targetType = targetType;
        this.randomInterval = reducedTickDelay(randomInterval);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.predicate = predicate;
    }

    public boolean canUse() {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        } else {
            if(Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) {
                return false;
            }

            this.scheduleFindTarget();
            return this.target != null;
        }
    }

    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    public void setTarget(@Nullable LivingEntity targetIn) {
        this.target = targetIn;
    }

    public void tick() {
        super.tick();

        if(Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER){
            return;
        }

        if(target == null || isInFight(this.target)) this.scheduleFindTarget();
    }

    private boolean isInFight(LivingEntity living) {
        if (living != null) {
            int targetLastHurt = mob.tickCount - living.getLastHurtByMobTimestamp();
            LivingEntity targetHurtEntity = living.getLastHurtByMob();

            if (targetHurtEntity != null && !targetHurtEntity.equals(this.mob)) {
                return targetLastHurt < 15;
            }
        }
        return false;
    }

    private void scheduleFindTarget() {
        if(findTarget != null && !findTarget.isProcessed()) return;

        FindTarget<T> findTarget = new FindTarget<>(this.mob, this.targetType, this.getFollowDistance(), this.predicate);
        this.findTarget = findTarget;
        FindTargetProcessor.queue(findTarget);
        FindTargetProcessor.awaitProcessing(findTarget, this.mob.getServer(), (FindTarget<T> processedFindTarget) -> {
            if(processedFindTarget != findTarget) {
                return;
            }

            this.target = processedFindTarget.getTarget();
        });
    }
}
