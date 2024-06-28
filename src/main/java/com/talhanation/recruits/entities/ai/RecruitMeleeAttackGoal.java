package com.talhanation.recruits.entities.ai;


import com.talhanation.recruits.entities.AbstractRecruitEntity;

import com.talhanation.recruits.util.AttackUtil;
import net.minecraft.core.BlockPos;
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
    private long lastCanUseCheck;
    private final double range;
    public RecruitMeleeAttackGoal(AbstractRecruitEntity recruit, double speedModifier, double range) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;

        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        //check if last use was 10 tick before
        long i = this.recruit.level.getGameTime();
        if (i - this.lastCanUseCheck >= 10L) {
            this.lastCanUseCheck = i;

            LivingEntity target = this.recruit.getTarget();
            if (target != null && target.isAlive()) {
                boolean isClose = target.distanceTo(this.recruit) <= range;
                boolean canSee = this.recruit.getSensing().hasLineOfSight(target);
                if (isClose && canSee && canAttackHoldPos() && recruit.getState() != 3 && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos()) {
                    this.path = this.recruit.getNavigation().createPath(target, 0);
                    if (this.path != null) {
                        return true;
                    } else {
                        double distance = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
                        double reach = AttackUtil.getAttackReachSqr(recruit);
                        return (reach >=  distance) && canAttackHoldPos();
                    }
                }
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.recruit.getTarget();

        if (target == null) {
            return false;
        }
        else if (!target.isAlive() && !this.recruit.getSensing().hasLineOfSight(target)) {
            return false;
        }
        else if (!this.recruit.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        else {
            boolean canAttackHoldPos = canAttackHoldPos();
            boolean needsToGetFood = recruit.needsToGetFood();
            boolean getShouldMount = recruit.getShouldMount();
            boolean getShouldMovePos = recruit.getShouldMovePos();

            return (!(target instanceof Player) || !target.isSpectator() && !((Player)target).isCreative()) && canAttackHoldPos && recruit.getState() != 3 && !needsToGetFood && !getShouldMount && !getShouldMovePos;
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
        if(!recruit.isFollowing()) this.recruit.getNavigation().stop();
    }

    public void tick() {
        if(this.pathingCooldown > 0) this.pathingCooldown--;

        if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
            this.recruit.getJumpControl().jump();
        }

        LivingEntity target = this.recruit.getTarget();
        if(target != null && target.isAlive()){
            this.recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceToTarget = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
            double reach = AttackUtil.getAttackReachSqr(recruit);

            boolean canSee = this.recruit.getSensing().hasLineOfSight(target);
            boolean isNotFollowing = !recruit.isFollowing();
            boolean coolDownElapsed = this.pathingCooldown <= 0;

            if(distanceToTarget <= reach && this.recruit.getSensing().hasLineOfSight(target)){
                if(!recruit.isFollowing()) this.recruit.getNavigation().stop();
                AttackUtil.performAttack(this.recruit, target);
            }
            else if (canSee && isNotFollowing && coolDownElapsed) {

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
        LivingEntity target = this.recruit.getTarget();
        Vec3 pos = recruit.getHoldPos();

        if (target != null && pos != null && recruit.getShouldHoldPos()) {
            double distanceToPos = target.distanceToSqr(pos);

            return distanceToPos < 400;
        }
        return true;
    }
}
