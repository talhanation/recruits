package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;


public class RecruitRangedBowAttackGoal<T extends BowmanEntity> extends Goal {
    private final T recruit;
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
        this.recruit = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackIntervalMax = attackIntervalMax;
        this.attackRadius = attackRadius;
        this.stopRange = stopRange;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        this.consumeArrows = RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get();
    }

    public boolean canUse() {
        LivingEntity livingentity = this.recruit.getTarget();
        if (livingentity != null && livingentity.isAlive() && isHoldingBow(this.recruit)) {
            this.target = livingentity;
            float distance = this.target.distanceTo(this.recruit);
            // if (mob.getOwner() != null && mob.getShouldFollow() && mob.getOwner().distanceTo(this.recruit) <= 25.00D && !(target.distanceTo(this.recruit) <= 7.00D)) return false;
            boolean canTackMovePos = canAttackMovePos();
            //boolean notMounting = !mob.getShouldMount();
            boolean shouldRanged = this.recruit.getShouldRanged();
            boolean canAttack = this.recruit.canAttack(target);
            boolean notPassive = this.recruit.getState() != 3;
            boolean notNeedsToGetFood = !this.recruit.needsToGetFood();
            boolean canSee = this.recruit.getSensing().hasLineOfSight(target);
            if(!canSee){
                recruit.setTarget(null);
                return false;
            }
            return distance >= stopRange && canTackMovePos && notNeedsToGetFood && canAttack && notPassive && shouldRanged;
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
        return !consumeArrows || this.recruit.getInventory().hasAnyMatching(item -> item.is(ItemTags.ARROWS));
    }

    public boolean canContinueToUse() {
        return this.canUse() && isHoldingBow(this.recruit);
    }

    public void start() {
        super.start();
        this.recruit.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.recruit.setAggressive(false);
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.recruit.stopUsingItem();
    }

    public void tick() {
        if(target != null && target.isAlive()) {
            double distance = target.distanceToSqr(this.recruit);
            boolean isClose = distance <= 150;
            boolean isFar = distance >= 3500;
            boolean isTooFar = distance >= 4500;
            boolean inRange =  !isFar;
            boolean canSee = this.recruit.getSensing().hasLineOfSight(target);

            if (canSee) {
                ++this.seeTime;
            } else {
                this.seeTime = 0;
            }

            if(isTooFar){
                this.recruit.setTarget(null);
                this.stop();
                return;
            }

            // movement
            if (this.recruit.getShouldFollow() && this.recruit.getOwner() != null) {
                handleFollow(this.recruit.getOwner(), inRange, isFar, isClose);
            }
            else if(this.recruit.getShouldHoldPos() && this.recruit.getHoldPos() != null){
                handleHoldPos(this.recruit.getHoldPos(), inRange, isFar, isClose);
            }
            else {
                handleWander(inRange, isFar, isClose);
            }

            this.recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.recruit.isUsingItem()) {
                if (!canSee && this.seeTime < -60) {
                    this.recruit.stopUsingItem();
                }
                else if (canSee) {
                    int i = this.recruit.getTicksUsingItem();
                    if (i >= 20) {
                        this.recruit.stopUsingItem();
                        this.recruit.performRangedAttack(target, BowItem.getPowerForTime(i));
                        float f = Mth.sqrt((float) distance) / this.attackRadius;
                        this.attackTime = Mth.floor(f * (float) (this.attackIntervalMax - this.attackIntervalMin) + (float) this.attackIntervalMin);
                    }
                }
            }
            else if (--this.attackTime <= 0 && this.seeTime >= -60 && this.hasArrows()) {
                this.recruit.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
    }

    private boolean canAttackMovePos() {
        LivingEntity target = this.recruit.getTarget();
        BlockPos pos = this.recruit.getMovePos();

        if (target != null && pos != null && this.recruit.getShouldMovePos()) {
            boolean targetIsFar = target.distanceToSqr(this.recruit) >= 320;
            boolean posIsClose = pos.distSqr(this.recruit.getOnPos()) <= 150;
            boolean posIsFar = pos.distSqr(this.recruit.getOnPos()) > 150D;

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
        boolean ownerClose = owner.distanceToSqr(this.recruit) <= 100;

        if (ownerClose) {
            if (inRange) this.recruit.getNavigation().stop();
            if (isFar) this.recruit.getNavigation().moveTo(target, this.speedModifier);
            if (isClose) this.recruit.fleeEntity(target);
        }
        //if (!ownerClose) {
        //    this.recruit.getNavigation().moveTo(owner, this.speedModifier);
        //}
    }

    private void handleHoldPos(@NotNull Vec3 pos, boolean inRange, boolean isFar, boolean isClose){
        boolean posClose = pos.distanceToSqr(this.recruit.position()) <= 50;

        if (posClose) {
            if (inRange) this.recruit.getNavigation().stop();
        }
        else {
            //this.recruit.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1);
        }
        //if (!ownerClose) {
        //    this.recruit.getNavigation().moveTo(owner, this.speedModifier);
        //}
    }

    private void handleWander(boolean inRange, boolean isFar, boolean isClose){
        if (inRange) this.recruit.getNavigation().stop();
        if (isFar) this.recruit.getNavigation().moveTo(target, this.speedModifier);
        if (isClose) this.recruit.fleeEntity(target);
    }
}