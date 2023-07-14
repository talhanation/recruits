package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;


public class RecruitMeleeAttackGoal extends Goal {
    protected final AbstractRecruitEntity recruit;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private int ticksUntilMove;
    private long lastCanUseCheck;
    private int failedPathFindingPenalty = 0;
    private boolean canPenalize = false;

    public RecruitMeleeAttackGoal(AbstractRecruitEntity recruit, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
    }

    public boolean canUse() {
        long i = this.recruit.level.getGameTime();
        if (i - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = i;
            LivingEntity target = this.recruit.getTarget();
            if (target == null) {
                return false;
            }
            else if (!target.isAlive()) {
                return false;
            }
            else {
                if (canPenalize) {
                    if (--this.ticksUntilNextPathRecalculation <= 0) {
                        this.path = this.recruit.getNavigation().createPath(target, 0);
                        this.ticksUntilNextPathRecalculation = 4 + this.recruit.getRandom().nextInt(7);
                        return this.path != null;
                    } else {
                        return true;
                    }
                }
                this.path = this.recruit.getNavigation().createPath(target, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return (this.getAttackReachSqr(target) >= this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ())) && canAttackHoldPos() && recruit.getState() != 3 && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos();
                }
            }
        }
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.recruit.getTarget();

        if (target == null) {
            return false;
        }
        else if (!target.isAlive()) {
            return false;
        }
        else if (!this.followingTargetEvenIfNotSeen) {
            return !this.recruit.getNavigation().isDone();
        }
        else if (!this.recruit.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        else {
            return (!(target instanceof Player) || !target.isSpectator() && !((Player)target).isCreative()) && canAttackHoldPos() && !recruit.isFollowing() && this.recruit.canAttack(target) && this.recruit.getState() != 3;
        }
    }

    public void start() {
        LivingEntity target = this.recruit.getTarget();
        if ((!recruit.getShouldHoldPos()) || target.position().closerThan(recruit.position(), 12D)) {
            this.recruit.getNavigation().moveTo(this.path, this.speedModifier);
            this.recruit.setAggressive(true);
            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = getCooldownModifier();
            this.ticksUntilMove = 15;
        }
    }

    public void stop() {
        LivingEntity livingentity = this.recruit.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.recruit.setTarget(null);
        }

        this.recruit.setAggressive(false);
        this.recruit.getNavigation().stop();
    }

    public void tick() {
        LivingEntity target = this.recruit.getTarget();
        if(target != null && target.isAlive()) {
            //Main.LOGGER.info("this.ticksUntilNextAttack: " + this.ticksUntilNextAttack);

            double d0 = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.recruit.getSensing().hasLineOfSight(target)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.recruit.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.recruit.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                    if (this.recruit.getNavigation().getPath() != null) {
                        Node finalPathPoint = this.recruit.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
                            failedPathFindingPenalty = 0;
                        else
                            failedPathFindingPenalty += 5;
                    } else {
                        failedPathFindingPenalty += 5;
                    }
                }
                if (d0 > 1024.0D) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d0 > 256.0D) {
                    this.ticksUntilNextPathRecalculation += 5;
                }
            }

            double d2 = target.getEyeY();
            this.recruit.getLookControl().setLookAt(target.getX(), d2, target.getZ());
            this.recruit.lookAt(target, 10.0F, 10.0F);

            if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;
            if (this.ticksUntilNextAttack <= 0 && !this.recruit.swinging) {
                this.checkAndPerformAttack(target, d0);
            }
        }
    }

    protected void checkAndPerformAttack(LivingEntity target, double distance) {
        double d0 = this.getAttackReachSqr(target);
        if (distance <= d0){
            this.resetAttackCooldown();
            this.recruit.swing(InteractionHand.MAIN_HAND);
            this.recruit.doHurtTarget(target);
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = 5 + getCooldownModifier();
    }

    private int getCooldownModifier(){
        int modifier = 0;
        Item item = recruit.getMainHandItem().getItem();
        if(item instanceof TieredItem tieredItem && !(item instanceof SwordItem)){
            modifier = 3 - (int) tieredItem.getTier().getSpeed();
        }

        if (item instanceof AxeItem){
            modifier += 3;
        }

        return modifier;
    }

    protected double getAttackReachSqr(LivingEntity target) {
        float weaponWidth = 4F;
        //ItemStack stack = recruit.getMainHandItem();
        //CompoundTag tag = stack.getTag().getFloat("reach");

        return (double)(weaponWidth + this.recruit.getBbWidth() * 2.1F * this.recruit.getBbWidth() * 2.1F + target.getBbWidth());
    }

    private boolean canAttackHoldPos() {
        LivingEntity target = this.recruit.getTarget();
        BlockPos pos = recruit.getHoldPos();

        if (target != null && pos != null && recruit.getShouldHoldPos()) {
            boolean targetIsFar = target.distanceTo(this.recruit) >= 10.0D;
            boolean isFarToPos = !recruit.getHoldPos().closerThan(recruit.getOnPos(), 12D);
            if(targetIsFar) return false;
            else return !isFarToPos;
        }
        return true;
    }

}
