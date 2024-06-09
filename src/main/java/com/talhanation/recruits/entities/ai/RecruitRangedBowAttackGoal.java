package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.jetbrains.annotations.NotNull;

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
    private boolean consumeArrows;

    public RecruitRangedBowAttackGoal(T mob, double speedModifier, int attackIntervalMin, int attackIntervalMax, float attackRadius, double stopRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackIntervalMax = attackIntervalMax;
        this.attackRadius = attackRadius;
        this.stopRange = stopRange;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        this.consumeArrows = RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get();
    }

    public boolean canUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null && livingentity.isAlive() && isHoldingBow(mob)) {
            this.target = livingentity;
            float distance = this.target.distanceTo(this.mob);
            // if (mob.getOwner() != null && mob.getShouldFollow() && mob.getOwner().distanceTo(this.mob) <= 25.00D && !(target.distanceTo(this.mob) <= 7.00D)) return false;
            boolean canTackMovePos = canAttackMovePos();
            //boolean notMounting = !mob.getShouldMount();
            boolean canAttack = this.mob.canAttack(target);
            boolean notPassive = this.mob.getState() != 3;
            boolean notNeedsToGetFood = !mob.needsToGetFood();
            return distance >= stopRange && canTackMovePos && notNeedsToGetFood && canAttack && notPassive;
        } else {
            return false;
        }
    }

    public static boolean isHoldingBow(LivingEntity mob) {
        String name = mob.getMainHandItem().getDescriptionId();
        if(mob.isHolding(bow -> bow.is(Items.BOW))){
            return true;
        }
        else if (mob.isHolding(bow -> bow.getItem() instanceof BowItem))
            return true;

        else if (mob.isHolding(bow -> bow.getItem() instanceof ProjectileWeaponItem))
            return true;

        else
            return name.contains("bow");
    }

    private boolean hasArrows(){
        return !consumeArrows || this.mob.getInventory().hasAnyMatching(item -> item.is(ItemTags.ARROWS));
    }

    public boolean canContinueToUse() {
        return this.canUse() && isHoldingBow(mob);
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
        if(target != null && target.isAlive()) {
            double distance = target.distanceToSqr(this.mob);
            boolean isClose = distance <= 150;
            boolean isFar = distance >= 3500;
            boolean isTooFar = distance >= 4500;
            boolean inRange =  !isFar;
            boolean canSee = this.mob.getSensing().hasLineOfSight(target);

            if (canSee) {
                ++this.seeTime;
            } else {
                this.seeTime = 0;
            }

            if(isTooFar){
                this.mob.setTarget(null);
                this.stop();
                return;
            }

            // movement
            if (mob.getShouldFollow() && mob.getOwner() != null) {
                handleFollow(this.mob.getOwner(), inRange, isFar, isClose);
            }
            else if(mob.getShouldHoldPos() && mob.getHoldPos() != null){
                handleHoldPos(mob.getHoldPos(), inRange, isFar, isClose);
            }
            else {
                handleWander(inRange, isFar, isClose);
            }

            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.mob.isUsingItem()) {
                if (!canSee && this.seeTime < -60) {
                    this.mob.stopUsingItem();
                } else if (canSee) {
                    int i = this.mob.getTicksUsingItem();
                    if (i >= 20) {
                        this.mob.stopUsingItem();
                        this.mob.performRangedAttack(target, BowItem.getPowerForTime(i));
                        float f = Mth.sqrt((float) distance) / this.attackRadius;
                        this.attackTime = Mth.floor(f * (float) (this.attackIntervalMax - this.attackIntervalMin) + (float) this.attackIntervalMin);
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60 && this.hasArrows()) {
                this.mob.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
    }

    private boolean canAttackMovePos() {
        LivingEntity target = this.mob.getTarget();
        BlockPos pos = mob.getMovePos();

        if (target != null && pos != null && mob.getShouldMovePos()) {
            boolean targetIsFar = target.distanceToSqr(this.mob) >= 320;
            boolean posIsClose = pos.distSqr(this.mob.getOnPos()) <= 150;
            boolean posIsFar = pos.distSqr(this.mob.getOnPos()) > 150D;

            if (posIsFar) {
                return false;
            }

            else if (posIsClose && targetIsFar){
                return false;
            }
        }
        return true;
    }

    private void handleFollow(@NotNull LivingEntity owner, boolean inRange, boolean isFar, boolean isClose){
        boolean ownerClose = owner.distanceToSqr(this.mob) <= 100;

        if (ownerClose) {
            if (inRange) this.mob.getNavigation().stop();
            if (isFar) this.mob.getNavigation().moveTo(target, this.speedModifier);
            if (isClose) this.mob.fleeEntity(target);
        }
        //if (!ownerClose) {
        //    this.mob.getNavigation().moveTo(owner, this.speedModifier);
        //}
    }

    private void handleHoldPos(@NotNull BlockPos pos, boolean inRange, boolean isFar, boolean isClose){
        boolean posClose = pos.distSqr(this.mob.getOnPos()) <= 50;

        if (posClose) {
            if (inRange) this.mob.getNavigation().stop();
        }
        else {
            //this.mob.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1);
        }
        //if (!ownerClose) {
        //    this.mob.getNavigation().moveTo(owner, this.speedModifier);
        //}
    }

    private void handleWander(boolean inRange, boolean isFar, boolean isClose){
        if (inRange) this.mob.getNavigation().stop();
        if (isFar) this.mob.getNavigation().moveTo(target, this.speedModifier);
        if (isClose) this.mob.fleeEntity(target);
    }
}