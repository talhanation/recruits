package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.IBoatController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
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

        this.oldWaterMalus = mob.getPathfindingMalus(BlockPathTypes.WATER);
        mob.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);

        this.oldBlockedMalus = mob.getPathfindingMalus(BlockPathTypes.BLOCKED);
        mob.setPathfindingMalus(BlockPathTypes.BLOCKED, -1F);

        this.oldBreachMalus = mob.getPathfindingMalus(BlockPathTypes.BREACH);
        mob.setPathfindingMalus(BlockPathTypes.BREACH, 0.0F);

        Entity vehicle = this.mob.getVehicle();
        float width = vehicle != null ? vehicle.getBbWidth() + 3.0F : mob.getBbWidth() + 1.0F;
        float height = vehicle != null ? vehicle.getBbHeight() + 1.0F : mob.getBbHeight() + 1.0F;

        this.entityWidth = Mth.floor(width);
        this.entityHeight = Mth.floor(height);
    }


    @Nullable
    protected Node getNode(int x, int y, int z) {
        Node node = null;
        BlockPathTypes blockpathtypes = this.getCachedBlockType(x, y, z);
        if (blockpathtypes == BlockPathTypes.WATER || blockpathtypes == BlockPathTypes.BREACH) {
            float f = this.mob.getPathfindingMalus(blockpathtypes);
            if (f >= 0.0F) {
                node = this.getNodeRaw(x, y, z);
                if (node != null) {
                    node.type = blockpathtypes;
                    node.costMalus = Math.max(node.costMalus, f);
                    BlockPos pos = new BlockPos(x, y, z);
                    //BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

                    for(Direction direction : Direction.Plane.HORIZONTAL) {
                        //mutablePos.set(pos).move(direction);
                        //BlockState belowStateNeighbors = this.level.getBlockState(mutablePos.below());

                        if (this.level.getFluidState(pos.relative(direction,2)).isEmpty()) {
                            node.costMalus += 4.0F;
                        }

                        if (this.level.getFluidState(pos.relative(direction,4)).isEmpty()) {
                            node.costMalus += 8.0F;
                        }

                        if (this.level.getFluidState(pos.relative(direction,6)).isEmpty()) {
                            node.costMalus += 16.0F;
                        }
                    }

                    if (this.level.getFluidState(pos.below(2)).isEmpty()){
                        node.costMalus += 4.0F;
                    }

                    if (this.level.getFluidState(pos.below(1)).isEmpty()){
                        node.costMalus += 8.0F;
                    }
                }
            }
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
        this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.BLOCKED, this.oldBlockedMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.BREACH, this.oldBreachMalus);
        super.done();
    }

    public @NotNull Node getStart() {
        boolean isWaterDeep = IBoatController.getWaterDepth(this.mob.getOnPos(), this.mob) > 3;
        AABB boundingBox = this.mob.getVehicle().getBoundingBox();

        double nodeX = isWaterDeep ? boundingBox.maxX : boundingBox.minX;
        //double nodeY = isWaterDeep ? boundingBox.maxY : boundingBox.minY;
        double nodeZ = isWaterDeep ? boundingBox.maxZ : boundingBox.minZ;
        return this.getNodeRaw(Mth.floor(nodeX), Mth.floor(this.mob.getY()), Mth.floor(nodeZ));
    }

    @Nullable
    public Target getGoal(double p_77550_, double p_77551_, double p_77552_) {
        return this.getTargetFromNode(this.getNodeRaw(Mth.floor(p_77550_), Mth.floor(p_77551_), Mth.floor(p_77552_)));
    }

    protected double getFloorLevel(@NotNull BlockPos p_164674_) {
        return this.mob.getY();
    }
}