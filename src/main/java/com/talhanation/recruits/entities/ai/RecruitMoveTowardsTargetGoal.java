package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;

public class RecruitMoveTowardsTargetGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private LivingEntity target;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private final double speedModifier;
    private final float within;

    public RecruitMoveTowardsTargetGoal(AbstractRecruitEntity recruit, double speedModifier, float within) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        this.target = this.recruit.getTarget();
        if (this.recruit.getShouldHoldPos()){
            return false;
        }

        if (this.recruit.getState() == 3){
            return false;
        }

        if (this.target == null) {
            return false;
        } else if (this.target.distanceToSqr(this.recruit) > (double)(this.within * this.within)) {
            return false;
        } else {
            Vector3d vector3d = RandomPositionGenerator.getPosTowards(this.recruit, 16, 7, this.target.position());
            if (vector3d == null) {
                return false;
            } else {
                this.wantedX = vector3d.x;
                this.wantedY = vector3d.y;
                this.wantedZ = vector3d.z;
                return true;
            }
        }
    }

    public boolean canContinueToUse() {
        if (this.recruit.getShouldFollow()){
            return false;
        }
        return !this.recruit.getNavigation().isDone() && this.target.isAlive() && this.target.distanceToSqr(this.recruit) < (double)(this.within * this.within);
    }

    public void stop() {
        this.target = null;
    }

    public void start() {
        if (!this.recruit.getShouldFollow()) {
        this.recruit.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
        }
    }

    public void tick(){
        if (this.target == null){
            stop();
        }
    }
}
