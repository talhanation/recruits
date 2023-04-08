package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RecruitWanderFreelyGoal extends Goal {
    private final AbstractRecruitEntity recruit;

    public RecruitWanderFreelyGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }
    public boolean canUse() {
        return recruit.getFollowState() == 0 && recruit.getHoldPos() != null && !recruit.getShouldUpkeep() && !recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    //maybe?? start(){
    public void tick() {
        BlockPos blockpos = this.recruit.getHoldPos();
        if (blockpos != null) {
            boolean isClose = recruit.distanceToSqr(blockpos.getX(), blockpos.getY(), blockpos.getZ()) <= this.recruit.getWanderRadius();

            if (!isClose) {
                this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1F);
            }
        }
    }
}

