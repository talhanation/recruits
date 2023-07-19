package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

import java.util.EnumSet;


public class RecruitRangedBowAttackGoal<T extends BowmanEntity & RangedAttackMob> extends Goal {
    private final T mob;
    private final double speedModifier;
    private final int attackIntervalMin;
    private LivingEntity target;
    private int attackTime = -1;
    private int seeTime;
    private final int attackIntervalMax;
    private final float attackRadius;
    private final double stopRange;

    public RecruitRangedBowAttackGoal(T mob, double speedModifier, int attackIntervalMin, int attackIntervalMax, float attackRadius, double stopRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackIntervalMax = attackIntervalMax;
        this.attackRadius = attackRadius;
        this.stopRange = stopRange;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null && livingentity.isAlive() && this.isHoldingBow()) {
            this.target = livingentity;
            // if (mob.getOwner() != null && mob.getShouldFollow() && mob.getOwner().distanceTo(this.mob) <= 25.00D && !(target.distanceTo(this.mob) <= 7.00D)) return false;
            return this.target.distanceTo(this.mob) >= stopRange && canAttackHoldPos() && canAttackMovePos() && !mob.needsToGetFood() && !mob.getShouldMount() && this.mob.canAttack(target) && this.mob.getState() != 3;
        } else {
            return false;
        }
    }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(is -> is.getItem() instanceof BowItem);
    }

    public boolean canContinueToUse() {
        return this.canUse() && this.isHoldingBow();
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.mob.stopUsingItem();
    }

    public void tick() {
        boolean isClose = target.distanceTo(this.mob) <= 7.00D;
        boolean isFar = target.distanceTo(this.mob) >= 21.5D;
        boolean inRange =  !isFar && target.distanceTo(this.mob) <= 15.0D;
        //if (mob.getHoldPos() != null)Objects.requireNonNull(this.mob.getOwner()).sendMessage(new StringTextComponent("Pos vorhanden"), mob.getOwner().getUUID());

        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        if (canSee) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        // movement

        if (mob.getShouldHoldPos() && mob.getHoldPos() != null) {
            if ((!mob.getHoldPos().closerThan(mob.getOnPos(), 5D))){
                if (inRange) this.mob.getNavigation().stop();
                if (isFar) this.mob.getNavigation().moveTo(target, this.speedModifier);
                //if (isClose) this.mob.fleeEntity(target); //buggt
            }
        }

        else if (mob.getShouldFollow() && mob.getOwner() != null){
            boolean playerClose = mob.getOwner().distanceTo(this.mob) <= 15.00D;

            if (playerClose){
                if (inRange) this.mob.getNavigation().stop();
                if (isFar) this.mob.getNavigation().moveTo(target, this.speedModifier);
                if (isClose) this.mob.fleeEntity(target);
            }
            if (!playerClose) {
                this.mob.getNavigation().moveTo(mob.getOwner(), this.speedModifier);
            }
        }

        else {
            if (inRange) this.mob.getNavigation().stop();
            if (isFar) this.mob.getNavigation().moveTo(target, this.speedModifier);
            if (isClose) this.mob.fleeEntity(target);
        }

        double d0 = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.mob.isUsingItem()) {
            if (!canSee && this.seeTime < -60) {
                this.mob.stopUsingItem();
            }
            else if (canSee) {
                int i = this.mob.getTicksUsingItem();
                if (i >= 20) {
                    this.mob.stopUsingItem();
                    this.mob.performRangedAttack(target, BowItem.getPowerForTime(i));
                    float f = Mth.sqrt((float) d0) / this.attackRadius;
                    this.attackTime = Mth.floor(f * (float)(this.attackIntervalMax - this.attackIntervalMin) + (float)this.attackIntervalMin);
                }
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.BOW));
        }
    }

    private boolean canAttackHoldPos() {
        LivingEntity target = this.mob.getTarget();
        BlockPos pos = mob.getHoldPos();

        if (target != null && pos != null && mob.getShouldHoldPos()) {
            boolean targetIsFar = target.distanceTo(this.mob) >= 21.5D;
            boolean posIsClose = pos.distSqr(this.mob.getOnPos()) <= 15.0D;
            boolean posIsFar = pos.distSqr(this.mob.getOnPos()) > 15.0D;

            if (posIsFar) {
                return false;
            }

            else if (posIsClose && targetIsFar){
                return false;
            }
        }
        return true;
    }

    private boolean canAttackMovePos() {
        LivingEntity target = this.mob.getTarget();
        BlockPos pos = mob.getMovePos();

        if (target != null && pos != null && mob.getShouldMovePos()) {
            boolean targetIsFar = target.distanceTo(this.mob) >= 21.5D;
            boolean posIsClose = pos.distSqr(this.mob.getOnPos()) <= 15.0D;
            boolean posIsFar = pos.distSqr(this.mob.getOnPos()) > 15.0D;

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