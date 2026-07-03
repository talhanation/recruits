package com.talhanation.recruits.entities.ai.navigation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import org.jetbrains.annotations.NotNull;

/**
 * Swim node evaluator for sailor recruits steering boats.
 *
 * <p>1.21 note: the pathfinder internals (node map, {@code getTarget}/{@code getNode},
 * {@code PathfindingContext}) were reworked, so the previous hand-copied node-cost
 * tweaks were dropped. The meaningful custom behaviour is preserved here: boat-aware
 * entity sizing plus a water/blocked/breach pathfinding-malus profile that keeps
 * boats moving through open water.</p>
 */
public class SailorNodeEvaluator extends SwimNodeEvaluator {
    private float oldWaterMalus;
    private float oldBlockedMalus;
    private float oldBreachMalus;

    public SailorNodeEvaluator() {
        super(false);
    }

    @Override
    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob) {
        super.prepare(region, mob);

        this.oldWaterMalus = mob.getPathfindingMalus(PathType.WATER);
        mob.setPathfindingMalus(PathType.WATER, 8.0F);

        this.oldBlockedMalus = mob.getPathfindingMalus(PathType.BLOCKED);
        mob.setPathfindingMalus(PathType.BLOCKED, -1F);

        this.oldBreachMalus = mob.getPathfindingMalus(PathType.BREACH);
        mob.setPathfindingMalus(PathType.BREACH, 0.0F);

        Entity vehicle = this.mob.getVehicle();
        float width = vehicle != null ? vehicle.getBbWidth() + 3.0F : mob.getBbWidth() + 1.0F;
        float height = vehicle != null ? vehicle.getBbHeight() + 1.0F : mob.getBbHeight() + 1.0F;

        this.entityWidth = Mth.floor(width);
        this.entityHeight = Mth.floor(height);
    }

    @Override
    public void done() {
        this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterMalus);
        this.mob.setPathfindingMalus(PathType.BLOCKED, this.oldBlockedMalus);
        this.mob.setPathfindingMalus(PathType.BREACH, this.oldBreachMalus);
        super.done();
    }
}
