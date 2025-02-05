package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.util.AttackUtil;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RecruitMeleeAttackGoal extends Goal {
    protected final AbstractRecruitEntity recruit;
    private final double speedModifier;
    private Path path;
    private int pathingCooldown;
    private final double range;

    public RecruitMeleeAttackGoal(AbstractRecruitEntity recruit, double speedModifier, double range) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;

        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity target = this.recruit.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        boolean isClose = target.distanceToSqr(this.recruit) <= range * range;
        boolean canSee = this.recruit.getSensing().hasLineOfSight(target);
        if (isClose && canSee && canAttackHoldPos() && recruit.getState() != 3 && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos()) {
            double distance = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
            this.path = this.recruit.getNavigation().createPath(target, 0);
            if (this.path != null) {
                return true;
            } else {
                double reach = AttackUtil.getAttackReachSqr(recruit);
                return (reach >= distance) && canAttackHoldPos();
            }
        }

        return false;
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.recruit.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        } else if (!this.recruit.getSensing().hasLineOfSight(target)) {
            return false;
        } else {
            boolean canAttackHoldPos = canAttackHoldPos();
            boolean needsToGetFood = recruit.needsToGetFood();
            boolean getShouldMount = recruit.getShouldMount();
            boolean getShouldMovePos = recruit.getShouldMovePos();
            return (!(target instanceof Player) || !target.isSpectator() && !((Player) target).isCreative()) && canAttackHoldPos && recruit.getState() != 3 && !needsToGetFood && !getShouldMount && !getShouldMovePos;
        }
    }

    public void start() {
        this.recruit.setAggressive(true);
        this.pathingCooldown = 0;
    }

    public void stop() {
        LivingEntity target = this.recruit.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.recruit.setTarget(null);
        }

        this.recruit.setAggressive(false);
        if (!recruit.isFollowing()) this.recruit.getNavigation().stop();
    }

    public void tick() {
        if (this.pathingCooldown > 0) this.pathingCooldown--;

        if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
            this.recruit.getJumpControl().jump();
        }

        LivingEntity target = this.recruit.getTarget();
        if (target != null && target.isAlive()) {
            this.recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceToTarget = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
            double reach = AttackUtil.getAttackReachSqr(recruit);

            boolean canSee = this.recruit.getSensing().hasLineOfSight(target);
            boolean isNotFollowing = !recruit.isFollowing();
            boolean coolDownElapsed = this.pathingCooldown <= 0;

            if (distanceToTarget <= reach && canSee) {
                if (isNotFollowing) this.recruit.getNavigation().stop();
                AttackUtil.performAttack(this.recruit, target);
            } else if (canSee && isNotFollowing && coolDownElapsed) {
                this.pathingCooldown = 4 + this.recruit.getRandom().nextInt(4);

                if (distanceToTarget > 2024.0D) {
                    this.pathingCooldown += 10;
                } else if (distanceToTarget > 256.0D) {
                    this.pathingCooldown += 5;
                }
                this.recruit.getNavigation().moveTo(target, this.speedModifier);
            }
        }
    }

    private boolean canAttackHoldPos() {
        Vec3 pos = recruit.getHoldPos();
        LivingEntity target = this.recruit.getTarget();
        if (target != null && pos != null && recruit.getShouldHoldPos()) {
            double distanceToPos = target.distanceToSqr(pos);
            double ref = recruit.isInFormation ? 169 : 400;

            return distanceToPos < ref;
        }
        return true;
    }
}
