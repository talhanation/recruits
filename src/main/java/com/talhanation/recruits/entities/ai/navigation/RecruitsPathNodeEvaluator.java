package com.talhanation.recruits.entities.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.*;
import org.jetbrains.annotations.NotNull;


public class RecruitsPathNodeEvaluator extends WalkNodeEvaluator {
    private int x;
    private int y;
    private int z;
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        if(mob.isVehicle()){
            this.entityHeight = Mth.floor(mob.getBbHeight() + (float) getEntityHeight());
        }
        mob.setPathfindingMalus(BlockPathTypes.WATER, 128.0F);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 128.0F);
        mob.setPathfindingMalus(BlockPathTypes.TRAPDOOR, -1.0F);
        mob.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
        mob.setPathfindingMalus(BlockPathTypes.DAMAGE_CAUTIOUS, 32.0F);
        mob.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        mob.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
        mob.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        mob.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
    }
    public RecruitsPathNodeEvaluator() {
        super();
    }

    private int getEntityHeight(){
        return mob.isVehicle() ? 2 : 1;
    }
    public void setTarget(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    //prefer blocks that have empty neighbors
    //prefer walk on dirt path
    //prefer walk on stairs when goal pos is higher or lower current position >2 blocks
    //prefer walk on ways with no leaves
    protected @NotNull Node getNode(int x, int y, int z) {
        Node node = super.getNode(x, y, z);
        BlockPathTypes blockpathtypes = this.getCachedBlockType(this.mob, x, y, z);
        float f = this.mob.getPathfindingMalus(blockpathtypes);

        if (f >= 0.0F) {
            node.type = blockpathtypes;
            node.costMalus = Math.max(node.costMalus, f);

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            BlockPos aboveEntityHeightPos = pos.above(getEntityHeight());

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                mutablePos.set(pos).move(direction);
                BlockState belowStateNeighbors = this.level.getBlockState(mutablePos.below());
                
                if (!belowStateNeighbors.is(Blocks.DIRT_PATH)) {
                    node.costMalus += 2.0F;
                    continue;
                }

                BlockState aboveLeavesCheck = this.level.getBlockState(aboveEntityHeightPos.relative(direction, 2));
                if (aboveLeavesCheck.is(BlockTags.LEAVES)) {
                    node.costMalus = -1.0F;
                    break;
                }
            }
        }


        return node;
    }
}