package com.talhanation.recruits.entities.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.*;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class RecruitsPathNodeEvaluator extends WalkNodeEvaluator {
    private int x;
    private int y;
    private int z;

    public RecruitsPathNodeEvaluator() {
        super();
    }

    public void setTarget(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob) {
        super.prepare(region, mob);
    }

    //prefer blocks that have empty neighbors
    //prefer walk on dirt path
    //prefer walk on stairs when goal pos is higher or lower current position >2 blocks

    protected @NotNull Node getNode(int x, int y, int z) {
        Node node = super.getNode(x,y,z);
        BlockPathTypes blockpathtypes = this.getCachedBlockType(this.mob, x, y, z);
        float f = this.mob.getPathfindingMalus(blockpathtypes);

        if (f >= 0.0F) {
            node.type = blockpathtypes;
            node.costMalus = Math.max(node.costMalus, f);
            BlockPos pos = new BlockPos(x, y, z);

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                if (!this.level.getBlockState(pos.below().relative(direction,1)).is(Blocks.DIRT_PATH)) {
                    node.costMalus += 2.0F;
                }

                if(Math.abs(this.y - mob.getY()) > 2) {
                    if (this.level.getBlockState(pos.relative(direction,1)).is(BlockTags.STAIRS)) {
                        node.costMalus = 0.0F;
                    }

                    if (this.level.getBlockState(pos.above().relative(direction,1)).is(BlockTags.STAIRS)) {
                        node.costMalus = 0.0F;
                    }

                    if (this.level.getBlockState(pos.below().relative(direction,1)).is(BlockTags.STAIRS)) {
                        node.costMalus = 0.0F;
                    }

                    for(int i = 0; i < 20; i++){
                        if (this.level.getBlockState(pos.relative(direction,i)).is(BlockTags.STAIRS)) {
                            node.costMalus = 0.0F;
                        }
                    }

                }
            }
        }
        return node;
    }

    private Node findStairs(){
        Node node = null;
        int range = 10;
        for(int i = -range; i < range; i++){
            for(int j = -range; j < range; j++){
                for(int k = -range; k < range; k++){
                    BlockPos pos = new BlockPos(i, j, k);
                    this.level.getBlockState(pos).is(BlockTags.STAIRS);
                    node = new Node(i,j,k);
                }
            }
        }
        return node;
    }
    /*

*/
}