package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;

import java.util.EnumSet;
import java.util.Objects;

public class HorseFollowOwnerGoal extends Goal {
    private final RecruitHorseEntity recruitHorse;
    private LivingEntity owner;
    private AbstractRecruitEntity rider;
    private final double speedModifier;
    private final PathNavigator navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;

    public HorseFollowOwnerGoal(RecruitHorseEntity horse, double v, float startDistance, float stopDistance) {
        this.recruitHorse = horse;
        this.rider = (AbstractRecruitEntity) horse.getControllingPassenger();
        this.speedModifier = v;
        this.navigation = horse.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity owner = this.recruitHorse.getOwner();
        if (owner == null) {
            return false;
        } else if (rider == null) {
            return false;
        } else if (owner.isSpectator()) {
            return false;
        }
        else if (rider.getFleeing()) {
            return false;
        } else if (!rider.getShouldFollow()) {
            return false;
        } else if (this.recruitHorse.distanceToSqr(owner) < (double)(this.startDistance * this.startDistance)) {
            return false;
        }
        else {
            this.owner = owner;
            return (rider.getShouldFollow());
        }
    }

    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (!(rider.getShouldFollow())){
            return false;
        }
        else {
            return (rider.getShouldFollow() && !(this.recruitHorse.distanceToSqr(this.owner) <= (double)(this.stopDistance * this.stopDistance)));
        }

    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.recruitHorse.getPathfindingMalus(PathNodeType.WATER);
        this.recruitHorse.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        //this.recruitHorse.setIsFollowing(true);
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.recruitHorse.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
        //this.recruitHorse.setIsFollowing(false);
    }

    public void tick() {
        this.recruitHorse.getLookControl().setLookAt(this.owner, 10.0F, (float)this.recruitHorse.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.recruitHorse.isLeashed() && !this.recruitHorse.isPassenger()) {
                    this.navigation.moveTo(this.owner, this.speedModifier);
            }

        }
    }
}
