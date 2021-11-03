package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

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
        else return this.recruit.getMovePos().closerThan(recruit.position(), within);
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    //maybe?? start(){
    public void tick() {
        BlockPos blockpos = this.recruit.getMovePos();
        if (blockpos != null && recruit.getMove()) {
            this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
        }

        if(blockpos.closerThan(recruit.position(), 3)){
            recruit.setShouldFollow(false);
            recruit.setMove(false);
        }
    }
}

