package com.talhanation.recruits.entities.ai;

import java.util.EnumSet;
import java.util.Objects;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class RecruitMeleeAttackGoal extends Goal {
    protected final AbstractRecruitEntity mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private final int attackInterval = 20;
    private long lastCanUseCheck;
    private int failedPathFindingPenalty = 0;
    private boolean canPenalize = false;

    public RecruitMeleeAttackGoal(AbstractRecruitEntity recruit, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.mob = recruit;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        long i = this.mob.level.getGameTime();
        if (i - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = i;
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return false;
            }
            else if (!target.isAlive()) {
                return false;
            }
            else {
                if (canPenalize) {
                    if (--this.ticksUntilNextPathRecalculation <= 0) {
                        this.path = this.mob.getNavigation().createPath(target, 0);
                        this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                        return this.path != null;
                    } else {
                        return true;
                    }
                }
                this.path = this.mob.getNavigation().createPath(target, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return (this.getAttackReachSqr(target) >= this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ())) && canAttackHoldPos() && mob.getState() != 3;
                }
            }
        }
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();

        if (target == null) {
            return false;
        }
        else if (!target.isAlive()) {
            return false;
        }
        else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        }
        else if (!this.mob.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        else {
            return (!(target instanceof PlayerEntity) || !target.isSpectator() && !((PlayerEntity)target).isCreative()) && canAttackHoldPos();
        }
    }

    public void start() {
        LivingEntity target = this.mob.getTarget();
        if ((!mob.getShouldHoldPos()) || target.position().closerThan(mob.position(), 8D)) {
            this.mob.getNavigation().moveTo(this.path, this.speedModifier);
            this.mob.setAggressive(true);
            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = 0;
        }
    }

    public void stop() {
        LivingEntity livingentity = this.mob.getTarget();
        if (!EntityPredicates.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.mob.setTarget(null);
        }

        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    public void tick() {
        LivingEntity livingentity = this.mob.getTarget();
        this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
        double d0 = this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().canSee(livingentity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.mob.getRandom().nextFloat() < 0.05F)) {
            this.pathedTargetX = livingentity.getX();
            this.pathedTargetY = livingentity.getY();
            this.pathedTargetZ = livingentity.getZ();
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
            if (this.canPenalize) {
                this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                if (this.mob.getNavigation().getPath() != null) {
                    net.minecraft.pathfinding.PathPoint finalPathPoint = this.mob.getNavigation().getPath().getEndNode();
                    if (finalPathPoint != null && livingentity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
                        failedPathFindingPenalty = 0;
                    else
                        failedPathFindingPenalty += 10;
                } else {
                    failedPathFindingPenalty += 10;
                }
            }
            if (d0 > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (d0 > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
            }

            if (!this.mob.getNavigation().moveTo(livingentity, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 15;
            }
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(livingentity, d0);
    }

    protected void checkAndPerformAttack(LivingEntity p_190102_1_, double p_190102_2_) {
        double d0 = this.getAttackReachSqr(p_190102_1_);
        if (p_190102_2_ <= d0 && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(Hand.MAIN_HAND);
            this.mob.doHurtTarget(p_190102_1_);
        }

    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = 20;
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }

    protected int getAttackInterval() {
        return 20;
    }

    protected double getAttackReachSqr(LivingEntity p_179512_1_) {
        return (double)(this.mob.getBbWidth() * 2.1F * this.mob.getBbWidth() * 2.1F + p_179512_1_.getBbWidth());
    }

    private boolean canAttackHoldPos() {
        LivingEntity target = this.mob.getTarget();
        BlockPos pos = mob.getHoldPos();

        if (target != null && pos != null && mob.getShouldHoldPos()) {
            boolean targetIsFar = target.distanceTo(this.mob) >= 10.0D;
            boolean posIsClose = pos.distSqr(this.mob.position(), false) <= 8.0D;
            boolean posIsFar = pos.distSqr(this.mob.position(),false) > 8.0D;

            if (posIsFar) {
                return false;
            }

            else if (posIsClose && targetIsFar){
                return false;
            }
        }
        return true;
    }

}
