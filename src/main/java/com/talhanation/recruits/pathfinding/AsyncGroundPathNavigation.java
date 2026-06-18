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

import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncGroundPathNavigation extends AsyncPathNavigation {
    // petal start — Generator ist statisch/final: er enthält keinen Zustand und kann geteilt werden
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
    }

    protected @NotNull PathFinder createPathFinder(int p_26453_) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);

        if (RecruitsServerConfig.UseAsyncPathfinding.get()) {
            return new AsyncPathfinder(this.nodeEvaluator, p_26453_, nodeEvaluatorGenerator, this.level);
        }
        return new PathFinder(this.nodeEvaluator, p_26453_);
    }

    protected boolean canUpdatePath() {
        return this.mob.onGround() || this.isInLiquid() || this.mob.isPassenger();
    }

    protected @NotNull Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
    }

    /**
     * Resolve the requested block into a standable target WITHOUT ever moving
     * the target upward.
     *
     * The vanilla/petal version walked upward out of solid blocks, which turned
     * an underground / in-cave target into a point on the surface above it — so
     * the recruit ran up the mountain instead of into the cave. We never do that
     * anymore:
     *
     *   - air target  -> drop down to the first standable block (so we path to
     *     the floor the target is standing on / above)
     *   - solid target-> drop down to the first open space with ground under it
     *     (i.e. step INTO the cave / opening), never climb out the top.
     *
     * If nothing standable is found downward we keep the original block and let
     * the pathfinder get as close as it can.
     */
    public Path createPath(BlockPos p_26475_, int p_26476_) {
        BlockPos resolved = resolveTargetDownwards(p_26475_);
        return super.createPath(resolved, p_26476_);
    }

    private BlockPos resolveTargetDownwards(BlockPos pos) {
        int minY = this.level.getMinBuildHeight();

        // Case A: target is in open space (air / non-solid). Find the floor it
        // belongs to by scanning DOWN to the first standable block.
        if (!this.level.getBlockState(pos).isSolid()) {
            BlockPos.MutableBlockPos m = pos.mutable();
            while (m.getY() > minY) {
                BlockState below = this.level.getBlockState(m.below());
                boolean hereOpen = !this.level.getBlockState(m).isSolid();
                if (hereOpen && below.isSolid()) {
                    return m.immutable(); // standable: open here, solid floor below
                }
                m.move(0, -1, 0);
            }
            return pos; // nothing solid below; keep as-is
        }

        // Case B: target is inside a solid block (typical underground / wall).
        // Scan DOWN looking for an open space that has a solid floor, so we step
        // into the cavity rather than climbing onto the surface above.
        BlockPos.MutableBlockPos m = pos.mutable();
        while (m.getY() > minY) {
            BlockState here = this.level.getBlockState(m);
            BlockState below = this.level.getBlockState(m.below());
            if (!here.isSolid() && below.isSolid()) {
                return m.immutable();
            }
            m.move(0, -1, 0);
        }
        return pos; // give up: let the pathfinder approach the solid block
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