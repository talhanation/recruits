package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
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
    private final int attackInterval = 20;
    private long lastCanUseCheck;
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;
    private int failedPathFindingPenalty = 0;
    private final double range;
    private final boolean canPenalize = true;

    public RecruitMeleeAttackGoal(AbstractRecruitEntity recruit, double speedModifier, boolean followingTargetEvenIfNotSeen, double range) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        long i = this.recruit.level.getGameTime();
        if (i - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        } else {
            this.lastCanUseCheck = i;
            LivingEntity target = this.recruit.getTarget();
            if (target == null) {
                return false;
            } else if (!target.isAlive()) {
                return false;
            } else {
                boolean isClose = target.distanceTo(this.recruit) <= range;
                if (isClose) {
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
                return false;
            }
        }
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.recruit.getTarget();
        if (target == null) {
            return false;
        } else if (!target.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.recruit.getNavigation().isDone();
        } else if (!this.recruit.isWithinRestriction(target.blockPosition())) {
            return false;
        } else {
            return !(target instanceof Player) || !target.isSpectator() && !((Player)target).isCreative();
        }
    }

    public void start() {
        this.recruit.getNavigation().moveTo(this.path, this.speedModifier);
        this.recruit.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    public void stop() {
        LivingEntity target = this.recruit.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.recruit.setTarget((LivingEntity)null);
        }

        this.recruit.setAggressive(false);
        this.recruit.getNavigation().stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity target = this.recruit.getTarget();
        if (target != null) {
            this.recruit.getLookControl().setLookAt(target);
            double d0 = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.recruit.getSensing().hasLineOfSight(target)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.recruit.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 2 + this.recruit.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                    if (this.recruit.getNavigation().getPath() != null) {
                        net.minecraft.world.level.pathfinder.Node finalPathPoint = this.recruit.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
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

                if (!this.recruit.getNavigation().moveTo(target, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }

                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }

            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            this.checkAndPerformAttack(target, d0);
        }
    }

    protected void checkAndPerformAttack(LivingEntity target, double p_190102_2_) {
        double d0 = this.getAttackReachSqr(target);
        if (p_190102_2_ <= d0 && isTimeToAttack()) {
            this.resetAttackCooldown();
            this.recruit.swing(InteractionHand.MAIN_HAND);
            this.recruit.doHurtTarget(target);
        }

    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = 20;
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;//get Weapon modifier
    }


    protected double getAttackReachSqr(LivingEntity target) {
        float weaponWidth = 3.75F;
        //if(Main.isAlexArmyInsatlled) getAlexArmoryModifier();
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

    /*
    private int getWeaponModifier(ItemStack stack){
        int base = 20;
        if(Main.isAlexArmouryModLoaded){
            String fullName = stack.getDescriptionId();
            int lastDotIndex = fullName.lastIndexOf(".");
            String result = fullName.substring(lastDotIndex + 1);
            AlexsArmoury weapon = AlexsArmoury.fromID(result);
            if(weapon != null){
                //attackSpeed > 1.6 --->
                base = 20 + Mth.ceil(weapon.attackSpeed * 20/1.6);
            }
            else return base;

        }
        return  0;
    }

     */
}
