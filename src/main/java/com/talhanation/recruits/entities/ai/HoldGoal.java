package com.talhanation.recruits.entities.ai;

import java.util.EnumSet;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

public class HoldGoal extends Goal {
    private final AbstractRecruitEntity entity;

    public HoldGoal(AbstractRecruitEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    public boolean canContinueToUse() {
        return this.entity.isOrderedToSit();
    }

    public boolean canUse() {
        if (!this.entity.isTame()) {
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
                return (!(this.entity.distanceToSqr(livingentity) < 144.0D) || livingentity.getLastHurtByMob() == null) && this.entity.isOrderedToSit();
            }
        }
    }

    public void start() {
        this.entity.getNavigation().stop();
        this.entity.setOrderedToSit(true);
    }

    public void stop() {
        this.entity.setOrderedToSit(false);
    }
}