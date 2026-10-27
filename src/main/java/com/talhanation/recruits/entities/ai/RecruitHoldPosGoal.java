package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RecruitHoldPosGoal extends Goal {
    /** Close enough to the hold position (0.2 blocks): stand still. */
    private static final double ARRIVED_SQR = 0.04D;
    /**
     * Within 1.5 blocks the recruit walks straight to the exact spot. The path navigation counts a node as
     * reached up to ~0.8 blocks away (and with Forge not even centered on the block), so with paths alone the
     * recruit stops next to his post and new paths to the same block end immediately.
     */
    private static final double FINE_APPROACH_SQR = 2.25D;

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

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        Vec3 pos = this.recruit.getHoldPos();
        if (pos == null) return;

        double dx = pos.x - recruit.getX();
        double dz = pos.z - recruit.getZ();
        double horizontal = dx * dx + dz * dz;
        double dy = pos.y - recruit.getY();
        boolean sameLevel = Math.abs(dy) < 1.2D;

        if (horizontal < ARRIVED_SQR && sameLevel) {
            if (!recruit.getNavigation().isDone()) recruit.getNavigation().stop();
            timeToRecalcPath = 0;
            return;
        }

        boolean fineApproach = horizontal < FINE_APPROACH_SQR && sameLevel
                && recruit.getVehicle() == null && !recruit.onClimbable() && !recruit.isInWater();
        if (fineApproach) {
            recruit.getNavigation().stop();
            // slow down on the last bit so he does not overshoot the spot
            double speed = recruit.moveSpeed * Math.max(0.4D, Math.min(1.0D, Math.sqrt(horizontal) * 1.5D));
            recruit.getMoveControl().setWantedPosition(pos.x, pos.y, pos.z, speed);
            if (recruit.horizontalCollision && dy > 0.5D) recruit.getJumpControl().jump();
            timeToRecalcPath = 0;
            return;
        }

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.recruit.getVehicle() != null ? this.adjustedTickDelay(5) : this.adjustedTickDelay(10);
            this.recruit.getNavigation().moveTo(pos.x(), pos.y(), pos.z(), this.recruit.moveSpeed);
        }

        this.recruit.tryJumpOverTerrain();
    }
}
