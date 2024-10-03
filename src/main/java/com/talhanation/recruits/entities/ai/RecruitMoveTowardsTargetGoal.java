package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.util.AttackUtil;
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
    }

    public boolean canUse() {
        this.target = this.recruit.getTarget();
        if (this.recruit.getState() == 3 || recruit.getShouldMount() || recruit.needsToGetFood() || this.recruit.isFollowing() || this.recruit.getShouldMovePos()){
            return false;
        }
        else if (this.target == null) {
            return false;
        }
        else if(this.recruit.isInFormation && this.target.distanceToSqr(this.recruit) > AttackUtil.getAttackReachSqr(recruit) * 1.5){
            return false;
        }
        else if (this.target.distanceToSqr(this.recruit) > (double)(this.within * this.within)) {
            return false;
        }
        else {
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
        if (this.recruit.isFollowing()){
            return false;
        }
        return !this.recruit.getNavigation().isDone() && this.target.isAlive() && this.target.distanceToSqr(this.recruit) < (double)(this.within * this.within);
    }

    public void stop() {
        this.target = null;
    }

    public void start() {
        if (!this.recruit.isFollowing()) {
            this.recruit.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
        }
    }

    public void tick(){
        if (this.target == null){
            stop();
        }
        if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
            this.recruit.getJumpControl().jump();
        }
    }
}
