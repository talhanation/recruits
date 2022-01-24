package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.world.IWorldReader;

import java.util.EnumSet;

public class RecruitFollowOwnerGoal extends Goal {
    private final AbstractRecruitEntity recruitEntity;
    private LivingEntity owner;
    private final IWorldReader level;
    private final double speedModifier;
    private final PathNavigator navigation;
    private int timeToRecalcPath;
    private final double stopDistance;
    private final double startDistance;
    private float oldWaterCost;

    public RecruitFollowOwnerGoal(AbstractRecruitEntity abstractRecruitEntity, double v, double startDistance, double stopDistance) {
        this.recruitEntity = abstractRecruitEntity;
        this.level = abstractRecruitEntity.level;
        this.speedModifier = v;
        this.navigation = abstractRecruitEntity.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity owner = this.recruitEntity.getOwner();
        if (owner == null) {
            return false;
        } else if (owner.isSpectator()) {
            return false;
        }
        else if (recruitEntity.getFleeing()) {
            return false;
        } else if (!this.recruitEntity.getShouldFollow()) {
            return false;
        } else if (this.recruitEntity.isOrderedToSit()) {
            return false;
        } else if (this.recruitEntity.distanceToSqr(owner) < (double)(this.startDistance * this.startDistance)) {
            return false;
        }
        else {
            this.owner = owner;
            return recruitEntity.getShouldFollow();
        }
    }

    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (this.recruitEntity.isOrderedToSit()) {
            return false;
        } else if (!this.recruitEntity.getShouldFollow()) {
            return false;
        }
        else {
            return recruitEntity.getShouldFollow() && !(this.recruitEntity.distanceToSqr(this.owner) <= (double)(this.stopDistance * this.stopDistance));
        }

    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.recruitEntity.getPathfindingMalus(PathNodeType.WATER);
        this.recruitEntity.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.recruitEntity.setIsFollowing(true);
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.recruitEntity.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
        this.recruitEntity.setIsFollowing(false);
    }

    public void tick() {
        this.recruitEntity.getLookControl().setLookAt(this.owner, 10.0F, (float)this.recruitEntity.getMaxHeadXRot());
        if (this.recruitEntity.isPassenger() && (recruitEntity.getVehicle() instanceof RecruitHorseEntity))
            this.navigation.moveTo(this.owner, this.speedModifier);

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.navigation.moveTo(this.owner, this.speedModifier);
        }
    }
}
