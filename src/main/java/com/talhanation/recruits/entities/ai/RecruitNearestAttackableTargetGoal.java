package com.talhanation.recruits.entities.ai;

import com.google.common.collect.Lists;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class RecruitNearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
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
    private static final int DEFAULT_RANDOM_INTERVAL = 10;
    protected final Class<T> targetType;
    protected final int randomInterval;

    @Nullable
    protected LivingEntity target;
    protected TargetingConditions targetConditionsNormal;

    public RecruitNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> predicate) {
        super(recruit, mustSee, mustReach);
        this.targetType = targetType;
        this.randomInterval = reducedTickDelay(randomInterval);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditionsNormal = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(predicate);
    }

    public boolean canUse() {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        } else {
            this.findTargetNormal();
            return this.target != null;
        }
    }

    protected AABB getTargetSearchArea(double range) {
        return this.mob.getBoundingBox().inflate(range, range, range);
    }

    protected void findTargetNormal() {
        List<T> list = this.mob.getCommandSenderWorld().getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()));

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
            if (anyTarget == null && d0 == -1.0D || d1 < d0) {
                anyTarget = t1.getTarget();
                d0 = d1;
            } else if (!t1.getInFight() && (anyD0 == -1.0D || d1 < anyD0)){
                target = t1.getTarget();
                anyD0 = d1;
            }
        }

        if (target != null) {
            this.target = target;
            return;
        }

        // We didn't find any target who is testified by target conditions and not fighting.
        // Try to find any target that testifies target conditions (does not matter if it fights or not)

        if (anyTarget != null) {
            this.target = anyTarget;
        }
        // We didn't find any target who is testified by target conditions.
        // So no targets buddy
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

        if (target != null && isInFight(target)) {
            findTargetNormal();
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
}
