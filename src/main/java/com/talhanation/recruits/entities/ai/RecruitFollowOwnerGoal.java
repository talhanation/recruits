package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RecruitFollowOwnerGoal extends Goal {
    private final AbstractRecruitEntity recruit;

    private final double speedModifier;
    private final double within;

    public RecruitFollowOwnerGoal(AbstractRecruitEntity recruit, double v, double within) {
        this.recruit = recruit;
        this.speedModifier = v;
        this.within = within;
    }

    public boolean canUse() {
        if (this.recruit.getOwner() == null) {
            return false;
        }
        else
            return this.recruit.getShouldFollow() && !recruit.getFleeing() && !recruit.needsToGetFood() && !recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void tick() {
        if (this.recruit.getOwner() != null){
            double distance = recruit.getOwner().distanceToSqr(recruit);
            if(recruit.getTarget() != null){
                follow(distance, 3, true);
            }
            else {
                follow(distance, 1, false);
            }

        }
    }

    private void follow(double distance, double multiplier, boolean target){
        if(distance > within * multiplier) {
            this.recruit.setIsFollowing(true);
            this.recruit.getNavigation().moveTo(recruit.getOwner().getX(), recruit.getOwner().getY(), recruit.getOwner().getZ(), this.speedModifier);
        }
        else{
            if(!target) this.recruit.getNavigation().stop();
            this.recruit.setIsFollowing(false);
        }
    }
}

