package com.talhanation.recruits.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class AsyncWaterBoundPathNavigation extends AsyncPathNavigation {
    private boolean allowBreaching;

    private static final NodeEvaluatorGenerator nodeEvaluatorGenerator = () -> {
        NodeEvaluator nodeEvaluator = new SwimNodeEvaluator(false);
        nodeEvaluator.setCanPassDoors(true);
        return nodeEvaluator;
    };

    public AsyncWaterBoundPathNavigation(AsyncPathfinderMob p_26515_, Level p_26516_) {
        super(p_26515_, p_26516_);
    }

    protected @NotNull AsyncPathfinder createPathFinder(int p_26598_) {
        this.allowBreaching = this.mob.getType() == EntityType.DOLPHIN;
        this.nodeEvaluator = new SwimNodeEvaluator(this.allowBreaching);
        return new AsyncPathfinder(this.nodeEvaluator, p_26598_, nodeEvaluatorGenerator, this.level);
    }

    protected boolean canUpdatePath() {
        return this.allowBreaching || this.isInLiquid();
    }

    protected @NotNull Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.mob.getY(0.5D), this.mob.getZ());
    }

    protected double getGroundY(Vec3 p_186136_) {
        return p_186136_.y;
    }

    protected boolean canMoveDirectly(@NotNull Vec3 p_186138_, @NotNull Vec3 p_186139_) {
        return isClearForMovementBetween(this.mob, p_186138_, p_186139_);
    }

    public boolean isStableDestination(@NotNull BlockPos p_26608_) {
        return !this.level.getBlockState(p_26608_).isSolidRender(this.level, p_26608_);
    }

    public void setCanFloat(boolean p_26612_) {
    }
}
