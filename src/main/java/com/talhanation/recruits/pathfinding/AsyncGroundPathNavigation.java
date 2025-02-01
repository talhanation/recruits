package com.talhanation.recruits.pathfinding;

import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncGroundPathNavigation extends AsyncPathNavigation {
    private static BiFunction<Integer, NodeEvaluator, PathFinder> pathfinderSupplier = (p_26453_, nodeEvaluator) -> new PathFinder(nodeEvaluator, p_26453_);
    // petal start
    private static final NodeEvaluatorGenerator nodeEvaluatorGenerator = () -> {
        NodeEvaluator nodeEvaluator = new WalkNodeEvaluator();
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanFloat(true);
        return nodeEvaluator;
    };
    // petal end

    private boolean avoidSun;

    public AsyncGroundPathNavigation(PathfinderMob p_26448_, Level p_26449_) {
        super(p_26448_, p_26449_);
        if(RecruitsServerConfig.UseAsyncPathfinding.get()) {
            pathfinderSupplier = (p_26453_, nodeEvaluator) -> new AsyncPathfinder(nodeEvaluator, p_26453_, nodeEvaluatorGenerator, this.level);
        }
    }

    protected @NotNull PathFinder createPathFinder(int p_26453_) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return pathfinderSupplier.apply(p_26453_, this.nodeEvaluator);
    }

    protected boolean canUpdatePath() {
        return this.mob.onGround() || this.isInLiquid() || this.mob.isPassenger();
    }

    protected @NotNull Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
    }

    public Path createPath(BlockPos p_26475_, int p_26476_) {
        if (this.level.getBlockState(p_26475_).isAir()) {
            BlockPos blockpos;
            for(blockpos = p_26475_.below(); blockpos.getY() > this.level.getMinBuildHeight() && this.level.getBlockState(blockpos).isAir(); blockpos = blockpos.below()) {}

            if (blockpos.getY() > this.level.getMinBuildHeight()) {
                return super.createPath(blockpos.above(), p_26476_);
            }

            while(blockpos.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(blockpos).isAir()) {
                blockpos = blockpos.above();
            }

            p_26475_ = blockpos;
        }

        if (!this.level.getBlockState(p_26475_).isSolid()) {
            return super.createPath(p_26475_, p_26476_);
        } else {
            BlockPos blockpos1;
            for(blockpos1 = p_26475_.above(); blockpos1.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(blockpos1).isSolid(); blockpos1 = blockpos1.above()) {
            }

            return super.createPath(blockpos1, p_26476_);
        }
    }

    public Path createPath(Entity p_26465_, int p_26466_) {
        return this.createPath(p_26465_.blockPosition(), p_26466_);
    }

    private int getSurfaceY() {
        if (this.mob.isInWater() && this.canFloat()) {
            int i = this.mob.getBlockY();
            BlockState blockstate = this.level.getBlockState(new BlockPos((int) this.mob.getX(), i, (int) this.mob.getZ()));
            int j = 0;

            while(blockstate.is(Blocks.WATER)) {
                ++i;
                blockstate = this.level.getBlockState(new BlockPos((int) this.mob.getX(), i, (int) this.mob.getZ()));
                ++j;
                if (j > 16) {
                    return this.mob.getBlockY();
                }
            }

            return i;
        } else {
            return Mth.floor(this.mob.getY() + 0.5D);
        }
    }

    protected void trimPath() {
        super.trimPath();
        if (this.avoidSun) {
            if (this.level.canSeeSky(new BlockPos((int) this.mob.getX(), (int) (this.mob.getY() + 0.5D), (int) this.mob.getZ()))) {
                return;
            }

            for(int i = 0; i < this.path.getNodeCount(); ++i) {
                Node node = this.path.getNode(i);
                if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
                    this.path.truncateNodes(i);
                    return;
                }
            }
        }

    }

    protected boolean hasValidPathType(BlockPathTypes p_26467_) {
        if (p_26467_ == BlockPathTypes.WATER) {
            return false;
        } else if (p_26467_ == BlockPathTypes.LAVA) {
            return false;
        } else {
            return p_26467_ != BlockPathTypes.OPEN;
        }
    }

    public void setCanOpenDoors(boolean p_26478_) {
        this.nodeEvaluator.setCanOpenDoors(p_26478_);
    }

    public boolean canPassDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setCanPassDoors(boolean p_148215_) {
        this.nodeEvaluator.setCanPassDoors(p_148215_);
    }

    public boolean canOpenDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setAvoidSun(boolean p_26491_) {
        this.avoidSun = p_26491_;
    }
}
