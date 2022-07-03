package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

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
        if (this.target != null && this.recruit.getShouldHoldPos() && this.target.distanceToSqr(this.recruit) > (double)(this.within * this.within)/2){
            return false;
        }

        if (this.target != null && !this.recruit.getShouldHoldPos() && this.target.distanceToSqr(this.recruit) > (double)(this.within * this.within)){
            return false;
        }

        if (this.recruit.getState() == 3){
            return false;
        }

        if (this.target == null) {
            return false;
        } else {
            Vec3 vec3 = DefaultRandomPos.getPosTowards(this.recruit, 16, 7, this.target.position(), (double)((float)Math.PI / 2F));
            if (vec3 == null) {
                return false;
            } else {
                this.wantedX = vec3.x;
                this.wantedY = vec3.y;
                this.wantedZ = vec3.z;
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
