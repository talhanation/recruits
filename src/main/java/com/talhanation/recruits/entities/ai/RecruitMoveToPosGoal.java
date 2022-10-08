package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;
import java.util.Objects;

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
        return (recruit.getShouldMovePos() && Objects.requireNonNull(this.recruit.getMovePos()).closerThan(recruit.getOnPos(), within));
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    //maybe?? start(){
    public void tick() {
        BlockPos blockpos = this.recruit.getMovePos();
        if (blockpos != null) {
            boolean isClose = recruit.distanceToSqr(blockpos.getX(), blockpos.getY(), blockpos.getZ()) <= 7.00D;

            this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);

            if (isClose) recruit.setShouldMovePos(false);
        }
    }
}

