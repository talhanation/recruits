package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.EnumSet;

public class RecruitHoldPosGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private final double speedModifier;
    private final double within;

    public RecruitHoldPosGoal(AbstractRecruitEntity recruit, double v, double within) {
      this.recruit = recruit;
      this.speedModifier = v;
      this.within = within;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    public boolean canUse() {
        if (this.recruit.getHoldPos() == null) {
            return false;
        }
        else if (recruit.getFollowState() != 2){
            return false;
        }
        else if (this.recruit.getHoldPos().closerThan(recruit.position(), within) && this.recruit.getShouldHoldPos())
            return true;
        else
            return false;
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void tick() {
        BlockPos blockpos = this.recruit.getHoldPos();
        if (this.recruit.getHoldPos() != null && canUse()) {
            this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
        }
    }
}
