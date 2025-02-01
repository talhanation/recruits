package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class RecruitWanderGoal extends Goal {
    public static final int DEFAULT_INTERVAL = 120;
    protected final AbstractRecruitEntity recruit;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    private long lastCanUseCheck;
    private int timeToRecalcPath;
    protected BlockPos initialPosition;
    public RecruitWanderGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    private boolean canWander(){
        return recruit.getFollowState() == 0 && recruit.getTarget() == null && !recruit.getShouldRest() && !recruit.getShouldMovePos() &&
                !recruit.getShouldProtect() && !recruit.getShouldFollow() && !recruit.getFleeing() && !recruit.needsToGetFood() && !recruit.getShouldMount();
    }
    public boolean canUse() {
        long i = this.recruit.getCommandSenderWorld().getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;
            
            return canWander();
        }
        return false;
    }

    public boolean canContinueToUse() {
        return this.canWander();
    }

    public void start() {
        this.initialPosition = recruit.getOnPos();
        timeToRecalcPath = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if(!recruit.getNavigation().isDone()){
            if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                this.recruit.getJumpControl().jump();
            }
        }

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(200) + recruit.getRandom().nextInt(50);


            Vec3 vec3 = this.getPosition();
            if (vec3 != null) {
                this.wantedX = vec3.x;
                this.wantedY = vec3.y;
                this.wantedZ = vec3.z;

                this.recruit.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, 0.85);
            }
        }
    }

    public void stop() {
        this.recruit.getNavigation().stop();
        this.initialPosition = null;
        super.stop();
    }


    @Nullable
    protected Vec3 getPosition() {
        if (this.recruit.isInWaterOrBubble()) {
            this.recruit.restrictTo(initialPosition, 150);
            Vec3 vec3 = LandRandomPos.getPos(this.recruit, 32, 16);
            return vec3 == null ? DefaultRandomPos.getPos(this.recruit, 32, 16) : vec3;
        }
        else {
            this.recruit.restrictTo(initialPosition, 20);
            return LandRandomPos.getPos(this.recruit, 10, 0);
        }
    }
}
