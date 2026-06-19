package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class RecruitMoveToPosGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private final double speedModifier;
    private int timeToRecalcPath;

    /** ticks spent actively trying to reach the current order. */
    private int activeTicks;
    /** true once we have actually issued at least one pathfind for this order. */
    private boolean wasAlreadyTrying;
    /**
     * Only allow "give up because nav is exhausted" after we have genuinely been
     * trying for a bit. Prevents bailing on the very first tick, when the
     * navigation is trivially "done" because no path exists yet.
     */
    private static final int GIVE_UP_GRACE_TICKS = 60;

    public RecruitMoveToPosGoal(AbstractRecruitEntity recruit, double v) {
        this.recruit = recruit;
        this.speedModifier = v;
    }

    @Override
    public void start() {
        super.start();
        this.timeToRecalcPath = 0;
        this.activeTicks = 0;
        this.wasAlreadyTrying = false;
    }

    public boolean canUse() {
        return recruit.getShouldMovePos() && !recruit.needsToGetFood() && !recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    //maybe?? start(){
    public void tick() {
        BlockPos blockpos = this.recruit.getMovePos();
        if (blockpos != null) {
            this.activeTicks++;
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.recruit.getVehicle() != null ? this.adjustedTickDelay(5) : this.adjustedTickDelay(10);

                // IMPORTANT: use the full 3D distance (including the target Y),
                // not recruit.getY(). The old horizontal-only check declared the
                // move "done" as soon as the recruit was roughly above/below the
                // target, so deep or underground targets were never actually
                // reached. Follow worked because it measured true distance to the
                // owner entity; now move matches that behaviour.
                double distanceSqr = recruit.distanceToSqr(
                        blockpos.getX() + 0.5D,
                        blockpos.getY(),
                        blockpos.getZ() + 0.5D);

                // ~1.5 block arrival radius (1.5^2 = 2.25). Tighten/loosen here.
                if (distanceSqr > 2.25D) {
                    boolean navStuck = this.recruit.getNavigation() instanceof AsyncPathNavigation async && async.isStuck();
                    boolean navDone = this.recruit.getNavigation().isDone();

                    // The move target is static, so a valid in-progress path does
                    // not need recomputing. Only path when we have no active path
                    // (or the last one finished short / we are stuck). This avoids
                    // a fresh pathfind every recalc tick for every recruit, which
                    // is the main source of spikes in big battles.
                    if (navDone || navStuck) {
                        // Navigation has nothing to follow. If it has genuinely
                        // given up after a grace period, accept the closest spot
                        // instead of recomputing forever.
                        if (this.activeTicks >= GIVE_UP_GRACE_TICKS && this.wasAlreadyTrying) {
                            recruit.setShouldMovePos(false);
                            recruit.clearMovePos();
                            recruit.reachedMovePos = true;
                            return;
                        }

                        this.recruit.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
                        this.wasAlreadyTrying = true;
                    }

                    if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                        this.recruit.getJumpControl().jump();
                    }
                }
                else {
                    recruit.setShouldMovePos(false);
                    recruit.clearMovePos();
                    recruit.reachedMovePos = true;
                }
            }
        }
    }
}