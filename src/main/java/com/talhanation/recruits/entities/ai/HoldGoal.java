package com.talhanation.recruits.entities.ai;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.TameableEntity;

public class HoldGoal
//        extends Goal
{
/*    private final HireAbleEntity entity;

    public HoldGoal(HireAbleEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    public boolean canContinueToUse() {
        return this.entity.isOrderedToSit();
    }

    public boolean canUse() {
        if (!this.entity.isHired()) {
            return false;
        } else if (this.entity.isInWaterOrBubble()) {
            return false;
        } else if (!this.entity.isOnGround()) {
            return false;
        } else {
            LivingEntity livingentity = this.entity.getOwner();
            if (livingentity == null) {
                return true;
            } else {
                return this.entity.distanceToSqr(livingentity) < 144.0D && livingentity.getLastHurtByMob() != null ? false : this.entity.isOrderedToHold();
            }
        }
    }

    public void start() {
        this.entity.getNavigation().stop();
        this.entity.setInSittingPose(true);
    }

    public void stop() {
        this.entity.setInSittingPose(false);
    }
    */
}