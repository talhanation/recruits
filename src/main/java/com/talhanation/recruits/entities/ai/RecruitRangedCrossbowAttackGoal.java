package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class RecruitRangedCrossbowAttackGoal<T extends CrossBowmanEntity & CrossbowAttackMob> extends Goal {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private final T recruit;
    private RecruitRangedCrossbowAttackGoal.CrossbowState crossbowState = RecruitRangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;

    public RecruitRangedCrossbowAttackGoal(T p_25814_, double p_25815_, float p_25816_) {
        this.recruit = p_25814_;
        this.speedModifier = p_25815_;
        this.attackRadiusSqr = p_25816_ * p_25816_;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.recruit.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    public boolean canContinueToUse() {
        return this.isValidTarget() && (this.canUse() || !this.recruit.getNavigation().isDone()) && this.isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.recruit.getTarget() != null && this.recruit.getTarget().isAlive();
    }

    public void stop() {
        super.stop();
        this.recruit.setAggressive(false);
        this.recruit.setTarget((LivingEntity)null);
        this.seeTime = 0;
        if (this.recruit.isUsingItem()) {
            this.recruit.stopUsingItem();
            this.recruit.setChargingCrossbow(false);
            CrossbowItem.setCharged(this.recruit.getUseItem(), false);
        }

    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity livingentity = this.recruit.getTarget();
        if (livingentity != null) {
            boolean flag = this.recruit.getSensing().hasLineOfSight(livingentity);
            boolean flag1 = this.seeTime > 0;
            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            double d0 = this.recruit.distanceToSqr(livingentity);
            boolean flag2 = (d0 > (double)this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
            if (flag2) {
                --this.updatePathDelay;
                if (this.updatePathDelay <= 0) {
                    this.recruit.getNavigation().moveTo(livingentity, this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.recruit.getRandom());
                }
            } else {
                this.updatePathDelay = 0;
                this.recruit.getNavigation().stop();
            }

            this.recruit.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            if (this.crossbowState == RecruitRangedCrossbowAttackGoal.CrossbowState.UNCHARGED) {
                if (!flag2) {
                    this.recruit.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.recruit, item -> item instanceof CrossbowItem));
                    this.crossbowState = RecruitRangedCrossbowAttackGoal.CrossbowState.CHARGING;
                    this.recruit.setChargingCrossbow(true);
                }
            } else if (this.crossbowState == RecruitRangedCrossbowAttackGoal.CrossbowState.CHARGING) {
                if (!this.recruit.isUsingItem()) {
                    this.crossbowState = RecruitRangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
                }

                int i = this.recruit.getTicksUsingItem();
                ItemStack itemstack = this.recruit.getUseItem();
                if (i >= CrossbowItem.getChargeDuration(itemstack)) {
                    this.recruit.releaseUsingItem();
                    this.crossbowState = RecruitRangedCrossbowAttackGoal.CrossbowState.CHARGED;
                    this.attackDelay = 20 + this.recruit.getRandom().nextInt(20);
                    this.recruit.setChargingCrossbow(false);
                }
            } else if (this.crossbowState == RecruitRangedCrossbowAttackGoal.CrossbowState.CHARGED) {
                --this.attackDelay;
                if (this.attackDelay == 0) {
                    this.crossbowState = RecruitRangedCrossbowAttackGoal.CrossbowState.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == RecruitRangedCrossbowAttackGoal.CrossbowState.READY_TO_ATTACK && flag) {
                this.recruit.performRangedAttack(livingentity, 1.0F);
                ItemStack itemstack1 = this.recruit.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this.recruit, item -> item instanceof CrossbowItem));
                CrossbowItem.setCharged(itemstack1, false);
                this.crossbowState = RecruitRangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
            }

        }
    }

    private boolean canRun() {
        return this.crossbowState == RecruitRangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
    }

    static enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;
    }
}
