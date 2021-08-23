package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.EnumSet;
import java.util.Optional;

public class RecruitMoveToPosGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private final double speedModifier;
    private final double within;

    public RecruitMoveToPosGoal(AbstractRecruitEntity recruit, double v, double within) {
        this.recruit = recruit;
        this.speedModifier = v;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    public boolean canUse() {
        if (this.recruit.getMovePos() == null && recruit.getMove()) {
            return false;
        }
        else if (this.recruit.getMovePos().closerThan(recruit.position(), within))
            return true;
        else
            return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void tick() {
        BlockPos blockpos = this.recruit.getMovePos();
        if (blockpos != null && recruit.getMove()) {
            this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
        }

        if(blockpos.closerThan(recruit.position(), 1)){
            recruit.setFollow(0);
            recruit.setMove(false);
        }
    }
}

