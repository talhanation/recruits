package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathNodeType;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class RecruitFollowHeroGoal extends Goal {
    public final AbstractRecruitEntity recruit;
    public double stopDistance;
    public double speedModifier;
    private LivingEntity owner;

    public RecruitFollowHeroGoal(AbstractRecruitEntity recruit, double v, double stopDistance) {
        this.recruit = recruit;
        this.stopDistance = stopDistance;
        this.owner = recruit.getOwner();
        this.speedModifier = v;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void start() {
        this.recruit.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.recruit.getNavigation().moveTo(Objects.requireNonNull(this.recruit.getOwner()), this.speedModifier);
    }

    public void tick() {
        if (owner != null) {
            this.recruit.getNavigation().moveTo(owner, speedModifier);
            this.recruit.getLookControl().setLookAt(this.owner, 10.0F, (float) this.recruit.getMaxHeadXRot());
        }
    }

    public boolean canUse() {
        if (owner != null) {
            if (owner.isSpectator())
                return false;
        }
        return (this.recruit.getFollow() == 1 && shouldExecute());
    }

    public boolean shouldExecute() {
        List<LivingEntity> list = this.recruit.level.getEntitiesOfClass(PlayerEntity.class, this.recruit.getBoundingBox().inflate(40.0D));
        if (!list.isEmpty())
            for (LivingEntity mob : list) {
                PlayerEntity player = (PlayerEntity) mob;
                if (!player.isInvisible() && recruit.isOwnedByThisPlayer(recruit, player)) {
                    if (this.recruit.getFollow() == 1)
                        return true;
                }
            }
        return false;
    }

    public void stop() {
        this.recruit.getNavigation().stop();
        //this.recruit.setPathfindingMalus(PathNodeType.WATER);
    }

    public boolean canContinueToUse() {
        LivingEntity owner = recruit.getOwner();
        if (owner != null) {
            if (this.recruit.getNavigation().isDone()) {
                return false;
            } else if (this.recruit.isOrderedToSit()) {
                return false;
            } else if (this.recruit.getFollow() == 0) {
                return false;
            }
        }
        return true;
    }
}
