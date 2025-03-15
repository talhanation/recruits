package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RecruitHoldPosGoal extends Goal {
    private final AbstractRecruitEntity recruit;

    private int timeToRecalcPath;

    public RecruitHoldPosGoal(AbstractRecruitEntity recruit, double within) {
      this.recruit = recruit;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public void start() {
        super.start();
        timeToRecalcPath = 0;
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
        Vec3 pos = this.recruit.getHoldPos();
        if (pos != null) {
            double distance = recruit.distanceToSqr(pos);
            if(distance >= 0.3) {
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.recruit.getVehicle() != null ? this.adjustedTickDelay(5) : this.adjustedTickDelay(10);
                    this.recruit.getNavigation().moveTo(pos.x(), pos.y(), pos.z(), this.recruit.moveSpeed);
                }

                if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                    this.recruit.getJumpControl().jump();
                }
            } else{
                if(recruit.rotate){
                    this.recruit.setYRot(recruit.ownerRot);
                    this.recruit.yRotO = this.recruit.ownerRot;
                    this.recruit.yBodyRot = this.recruit.ownerRot;
                    this.recruit.yHeadRot = this.recruit.ownerRot;
                    recruit.rotate = false;
                }
            }
        }
    }
}
