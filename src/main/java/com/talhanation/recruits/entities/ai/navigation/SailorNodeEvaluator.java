package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.IBoatController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SailorNodeEvaluator extends WalkNodeEvaluator {
    private float oldWaterMalus;
    private float oldWalkableMalus;
    private float oldWaterBorderMalus;

    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob) {
        super.prepare(region, mob);

        this.oldWaterMalus = mob.getPathfindingMalus(BlockPathTypes.WATER);
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.oldWalkableMalus = mob.getPathfindingMalus(BlockPathTypes.WALKABLE);
        mob.setPathfindingMalus(BlockPathTypes.WALKABLE, -1F);
        this.oldWaterBorderMalus = mob.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);

        Entity vehicle = this.mob.getVehicle();
        float width = vehicle != null ? vehicle.getBbWidth() + 3.0F : mob.getBbWidth() + 1.0F;
        //float depth = vehicle != null ? vehicle.getBbWidth() + 3.0F : mob.getBbWidth() + 1.0F;
        float height = vehicle != null ? vehicle.getBbHeight() + 1.0F : mob.getBbHeight() + 1.0F;
        this.entityWidth = Mth.floor(width);
        //this.entityDepth = Mth.floor(depth);
        this.entityHeight = Mth.floor(height);
    }

    public void done() {
        this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.WALKABLE, this.oldWalkableMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, this.oldWaterBorderMalus);
        super.done();
    }


    public @NotNull Node getStart() {
        boolean isWaterDeep = IBoatController.getWaterDepth(this.mob.getOnPos(), this.mob) > 3;
        AABB boundingBox = this.mob.getVehicle().getBoundingBox();

        double nodeX = isWaterDeep ? boundingBox.maxX : boundingBox.minX;
        double nodeY = isWaterDeep ? boundingBox.maxY : boundingBox.minY;
        double nodeZ = isWaterDeep ? boundingBox.maxZ : boundingBox.minZ;
        return this.getNode(Mth.floor(nodeX), Mth.floor(nodeY), Mth.floor(nodeZ));
    }

    @Nullable
    public Target getGoal(double p_77550_, double p_77551_, double p_77552_) {
        return this.getTargetFromNode(this.getNode(Mth.floor(p_77550_), Mth.floor(p_77551_), Mth.floor(p_77552_)));
    }

    public int getNeighbors(Node @NotNull [] nodes, Node nodeIn) {
        int i = 0;
        int j = 0;
        BlockPathTypes cachedBlockType = this.getCachedBlockType(this.mob, nodeIn.x, nodeIn.y, nodeIn.z);

        double d0 = this.getFloorLevel(new BlockPos(nodeIn.x, nodeIn.y, nodeIn.z));
        Node node = this.findAcceptedNode(nodeIn.x, nodeIn.y, nodeIn.z + 1, j, d0, Direction.SOUTH, cachedBlockType);
        if (this.isNeighborValid(node, nodeIn)) {
            nodes[i++] = node;
        }

        Node node1 = this.findAcceptedNode(nodeIn.x - 1, nodeIn.y, nodeIn.z, j, d0, Direction.WEST, cachedBlockType);
        if (this.isNeighborValid(node1, nodeIn)) {
            nodes[i++] = node1;
        }

        Node node2 = this.findAcceptedNode(nodeIn.x + 1, nodeIn.y, nodeIn.z, j, d0, Direction.EAST, cachedBlockType);
        if (this.isNeighborValid(node2, nodeIn)) {
            nodes[i++] = node2;
        }

        Node node3 = this.findAcceptedNode(nodeIn.x, nodeIn.y, nodeIn.z - 1, j, d0, Direction.NORTH, cachedBlockType);
        if (this.isNeighborValid(node3, nodeIn)) {
            nodes[i++] = node3;
        }

        Node node4 = this.findAcceptedNode(nodeIn.x - 1, nodeIn.y, nodeIn.z - 1, j, d0, Direction.NORTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node1, node3, node4)) {
            nodes[i++] = node4;
        }

        Node node5 = this.findAcceptedNode(nodeIn.x + 1, nodeIn.y, nodeIn.z - 1, j, d0, Direction.NORTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node2, node3, node5)) {
            nodes[i++] = node5;
        }

        Node node6 = this.findAcceptedNode(nodeIn.x - 1, nodeIn.y, nodeIn.z + 1, j, d0, Direction.SOUTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node1, node, node6)) {
            nodes[i++] = node6;
        }

        Node node7 = this.findAcceptedNode(nodeIn.x + 1, nodeIn.y, nodeIn.z + 1, j, d0, Direction.SOUTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node2, node, node7)) {
            nodes[i++] = node7;
        }

        return i;
    }
    @Override
    protected boolean isDiagonalValid(Node node, @Nullable Node node1, @Nullable Node node2, @Nullable Node node3) {
        if (node3 != null && node2 != null && node1 != null) {
            if (node3.closed) {
                return false;
            } else if (node2.y <= node.y && node1.y <= node.y) {
                if (node1.type != BlockPathTypes.WALKABLE_DOOR && node2.type != BlockPathTypes.WALKABLE_DOOR && node3.type != BlockPathTypes.WALKABLE_DOOR) {
                    double width = this.mob.getVehicle() != null ? this.mob.getVehicle().getBbWidth() * 1.5 : this.mob.getBbWidth();
                    boolean flag = node2.type == BlockPathTypes.FENCE && node1.type == BlockPathTypes.FENCE && width < 0.5D;
                    return node3.costMalus >= 0.0F && (node2.y < node.y || node2.costMalus >= 0.0F || flag) && (node1.y < node.y || node1.costMalus >= 0.0F || flag);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected double getFloorLevel(@NotNull BlockPos p_164674_) {
        return this.mob.getY();
    }

    public @NotNull BlockPathTypes getBlockPathType(@NotNull BlockGetter blockGetter, int x, int y, int z) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPathTypes blockpathtypes = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(x, y, z));
        if (blockpathtypes == BlockPathTypes.WATER) {
            for(Direction direction : Direction.values()) {
                if(direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) continue;
                BlockPathTypes blockPathTypes = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(x, y, z).move(direction));
                if (blockPathTypes == BlockPathTypes.BLOCKED) {
                    return BlockPathTypes.WATER_BORDER;
                }
            }

            return BlockPathTypes.WATER;
        } else {
            return getBlockPathTypeStatic(blockGetter, mutableBlockPos);
        }
    }
}