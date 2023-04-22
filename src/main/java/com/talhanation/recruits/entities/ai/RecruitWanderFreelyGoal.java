package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RecruitWanderFreelyGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private BlockPos pos;

    public RecruitWanderFreelyGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }
    public boolean canUse() {
        return recruit.getTarget() != null && recruit.getFollowState() == 0 && !recruit.needsToGetFood() && !recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start(){
    super.start();
    this.pos = recruit.getOnPos();

    }
    public void tick() {

        if (pos != null) {
            boolean isClose = recruit.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= this.recruit.getWanderRadius();

            if (!isClose) {
                this.recruit.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1F);
            }
        }
    }
}

