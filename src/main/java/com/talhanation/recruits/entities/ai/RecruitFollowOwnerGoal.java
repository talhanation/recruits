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
            if(recruit.getOwner().distanceToSqr(recruit) > within) {
                this.recruit.setIsFollowing(true);
                this.recruit.getNavigation().moveTo(recruit.getOwner().getX(), recruit.getOwner().getY(), recruit.getOwner().getZ(), this.speedModifier);
            }
            else
                this.recruit.setIsFollowing(false);
        }
    }
}

