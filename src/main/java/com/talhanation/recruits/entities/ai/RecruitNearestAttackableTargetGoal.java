package com.talhanation.recruits.entities.ai;

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

        list.removeIf(target -> !this.targetConditionsNormal.test(this.mob, target));

        list.removeIf(this::isInFight);

        list.sort(Comparator.comparing(target -> target.distanceToSqr(this.mob)));

        if (!list.isEmpty()) {
            this.target = list.get(0);
        } else {
            // Fallback: if no unengaged targets are available, find any target
            if (this.targetType != Player.class && this.targetType != ServerPlayer.class) {
                this.target = this.mob.getCommandSenderWorld().getNearestEntity(this.mob.getCommandSenderWorld().getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()),
                        (p_148152_) -> true), this.targetConditionsNormal, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
            } else {
                this.target = this.mob.getCommandSenderWorld().getNearestPlayer(this.targetConditionsNormal, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
            }
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
