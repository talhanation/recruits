package com.talhanation.recruits.entities.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SailorNodeEvaluator extends SwimNodeEvaluator {
    private float oldWaterMalus;
    private float oldBlockedMalus;
    private float oldBreachMalus;

    public SailorNodeEvaluator() {
        super(false);
    }

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


    @Nullable
    protected Node getNode(int x, int y, int z) {
        PathType blockPathType = this.getCachedBlockType(x, y, z);

        if (blockPathType != PathType.WATER && blockPathType != PathType.BREACH) {
            return null;
        }

        float malus = this.mob.getPathfindingMalus(blockPathType);
        if (malus < 0.0F) {
            return null;
        }

        Node node = this.getNodeRaw(x, y, z);
        if (node == null) {
            return null;
        }

        node.type = blockPathType;
        node.costMalus = Math.max(node.costMalus, malus);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, y, z);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            mutablePos.set(x, y, z).move(direction, 2);
            if (this.currentContext.level().getFluidState(mutablePos).isEmpty()) {
                node.costMalus += 4.0F;
            }

            mutablePos.move(direction, 2);
            if (this.currentContext.level().getFluidState(mutablePos).isEmpty()) {
                node.costMalus += 8.0F;
            }

            mutablePos.move(direction, 2);
            if (this.currentContext.level().getFluidState(mutablePos).isEmpty()) {
                node.costMalus += 16.0F;
            }
        }

        mutablePos.set(x, y - 2, z);
        if (this.currentContext.level().getFluidState(mutablePos).isEmpty()) {
            node.costMalus += 4.0F;
        }

        mutablePos.set(x, y - 1, z);
        if (this.currentContext.level().getFluidState(mutablePos).isEmpty()) {
            node.costMalus += 8.0F;
        }

        return node;
    }


    @Nullable
    protected Node getNodeRaw(int x, int y, int z) {
        return this.nodes.computeIfAbsent(Node.createHash(x, y, z), (node) -> {
            return new Node(x, y, z);
        });
    }

    public void done() {
        this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterMalus);
        this.mob.setPathfindingMalus(PathType.BLOCKED, this.oldBlockedMalus);
        this.mob.setPathfindingMalus(PathType.BREACH, this.oldBreachMalus);
        super.done();
    }

    public @NotNull Node getStart() {
        AABB boundingBox = this.mob.getVehicle().getBoundingBox();

        double nodeX = boundingBox.minX; // Kein unnötiges Berechnen mehr!
        double nodeZ = boundingBox.minZ; // Kein unnötiges Berechnen mehr!

        return getNodeRaw(Mth.floor(nodeX), Mth.floor(this.mob.getY()), Mth.floor(nodeZ));
    }

    @Nullable
    public Target getTarget(double p_77550_, double p_77551_, double p_77552_) {
        return this.getTargetNodeAt(p_77550_, p_77551_, p_77552_);
    }
}