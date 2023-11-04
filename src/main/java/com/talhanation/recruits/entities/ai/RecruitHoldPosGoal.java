package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

public class RecruitHoldPosGoal extends Goal {
    private final AbstractRecruitEntity recruit;

    private final double speedModifier;

    public RecruitHoldPosGoal(AbstractRecruitEntity recruit, double v, double within) {
      this.recruit = recruit;
      this.speedModifier = v;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (this.recruit.getHoldPos() == null) {
            return false;
        }
        else
            return this.recruit.getShouldHoldPos() && !recruit.getFleeing() && !recruit.needsToGetFood() && !recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void tick() {
        BlockPos blockpos = this.recruit.getHoldPos();
        if (blockpos != null) {
            double distance = recruit.distanceToSqr(blockpos.getX(), blockpos.getY(), blockpos.getZ());
            if(distance >= 2) {
                this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);

                if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                    this.recruit.getJumpControl().jump();
                }

            }
            else if(distance > 1.25 ) this.recruit.getMoveControl().setWantedPosition(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
        }
    }
}
