package com.talhanation.recruits.pathfinding;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AsyncPathNavigation extends PathNavigation {
    @Nullable
    private BlockPos targetPos;
    private int reachRange;
    private final PathFinder pathFinder;
    private boolean isStuck;

    /**
     * True when the last processed path did NOT end exactly on the target, so we
     * only walked to the closest reachable point. We then keep retrying a fresh
     * path from the new position toward the real target until we land exactly
     * (or genuinely cannot get any closer).
     */
    private boolean awaitingExactRetry;
    private long lastExactRetry;
    /** end position of the previous fallback path, to detect "no progress". */
    @Nullable
    private BlockPos lastFallbackEnd;
    private int noProgressRetries;
    private static final int MAX_NO_PROGRESS_RETRIES = 3;

    public AsyncPathNavigation(PathfinderMob p_26515_, Level p_26516_) {
        super(p_26515_, p_26516_);
        int i = Mth.floor(p_26515_.getAttributeValue(Attributes.FOLLOW_RANGE) * 16.0D);
        this.pathFinder = this.createPathFinder(i);
    }

    protected @NotNull PathFinder createPathFinder(int p_26531_) {
        if (RecruitsServerConfig.UseAsyncPathfinding.get()) {
            return new AsyncPathfinder(this.nodeEvaluator, p_26531_, this.level);
        }
        return new PathFinder(this.nodeEvaluator, p_26531_);
    }

    @Nullable
    public final Path createPathAsync(double p_26525_, double p_26526_, double p_26527_, int p_26528_) {
        return this.createPath(new BlockPos((int) p_26525_, (int) p_26526_, (int) p_26527_), p_26528_);
    }

    @Nullable
    public Path createPath(Stream<BlockPos> p_26557_, int p_26558_) {
        return this.createPath(p_26557_.collect(Collectors.toSet()), 8, false, p_26558_);
    }


    @Nullable
    public Path createPath(Set<BlockPos> p_26549_, int p_26550_) {
        return this.createPath(p_26549_, 8, false, p_26550_);
    }


    @Nullable
    public Path createPath(BlockPos p_26546_, int p_26547_) {
        return this.createPath(ImmutableSet.of(p_26546_), 8, false, p_26547_);
    }


    @Nullable
    public Path createPath(BlockPos p_148219_, int p_148220_, int p_148221_) {
        return this.createPath(ImmutableSet.of(p_148219_), 8, false, p_148220_, (float) p_148221_);
    }


    @Nullable
    public Path createPath(Entity p_26534_, int p_26535_) {
        return this.createPath(ImmutableSet.of(p_26534_.blockPosition()), 16, true, p_26535_);
    }


    @Nullable
    protected Path createPath(Set<BlockPos> p_26552_, int p_26553_, boolean p_26554_, int p_26555_) {
        return this.createPath(p_26552_, p_26553_, p_26554_, p_26555_, (float) this.mob.getAttributeValue(Attributes.FOLLOW_RANGE));
    }


    @Nullable
    protected Path createPath(Set<BlockPos> p_148223_, int p_148224_, boolean p_148225_, int p_148226_, float p_148227_) {
        if(Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return null;
        if (p_148223_.isEmpty()) {
            return null;
        } else if (this.mob.getY() < (double) this.level.getMinBuildHeight()) {
            return null;
        } else if (!this.canUpdatePath()) {
            return null;
        } else if (this.path != null && (path instanceof AsyncPath asyncPath && !asyncPath.isProcessed() && asyncPath.hasSameProcessingPositions(p_148223_))) { // petal start - catch early if it's still processing these positions let it keep processing
            return this.path;
        } else if (this.path != null && !this.path.isDone() && p_148223_.contains(this.targetPos)) {
            return this.path;
        } else {
            BlockPos blockpos = p_148225_ ? this.mob.blockPosition().above() : this.mob.blockPosition();
            // Enlarge the region generously. A detour that actually lands on the
            // target can be much longer than the straight-line follow range, and
            // the region is a hard wall for the search, so we pad it well beyond
            // the old followRange+offset.
            int i = (int) ((p_148227_ + (float) p_148224_) * 1.5F) + 16;
            PathNavigationRegion pathnavigationregion = new PathNavigationRegion(this.level, blockpos.offset(-i, -i, -i), blockpos.offset(i, i, i));
            float maxVisitedNodesMultiplier = 1.0F;
            Path path = this.pathFinder.findPath(pathnavigationregion, this.mob, p_148223_, p_148227_, p_148226_, maxVisitedNodesMultiplier);

            if (!p_148223_.isEmpty())
                this.targetPos = p_148223_.iterator().next(); // petal - assign early a target position. most calls will only have 1 position

            // petal start - async
            if(RecruitsServerConfig.UseAsyncPathfinding.get()) {
                AsyncPathProcessor.awaitProcessing(path, this.level.getServer(), processedPath -> {
                    if (processedPath != this.path){
                        return; // petal - check that processing didn't take so long that we calculated a new path
                    }

                    if (processedPath != null) {
                        this.targetPos = processedPath.getTarget();
                        this.reachRange = p_148226_;
                        this.resetStuckTimeout();

                        boolean reachedExact = processedPath.canReach();
                        if (reachedExact) {
                            this.awaitingExactRetry = false;
                            this.noProgressRetries = 0;
                            this.lastFallbackEnd = null;
                        } else {
                            // Detect lack of progress: if this fallback ends at
                            // the same block as the previous one, we are not
                            // getting closer -> stop retrying after a few tries.
                            BlockPos end = processedPath.getEndNode() != null
                                    ? processedPath.getEndNode().asBlockPos()
                                    : null;
                            if (end != null && end.equals(this.lastFallbackEnd)) {
                                this.noProgressRetries++;
                            } else {
                                this.noProgressRetries = 0;
                            }
                            this.lastFallbackEnd = end;
                            this.awaitingExactRetry = this.noProgressRetries < MAX_NO_PROGRESS_RETRIES;
                        }
                    }
                });
            }
            return path;
        }
    }

    @Override
    public boolean moveTo(double p_26520_, double p_26521_, double p_26522_, double p_26523_) {
        // Route coordinate moves through the SAME path as moveTo(Entity) (used by
        // follow, which works very well): higher accuracy (16), canTargetUseTouch
        // = true, and the failure throttling below. Previously this used a leaner
        // createPath(pos, 1) with accuracy 8 and no touch, which is why move felt
        // worse than follow.
        if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return false;
        long currentTick = this.level.getGameTime();
        if (this.pathfindFailures > 10 && this.path == null && currentTick < this.lastFailure + 40) {
            return false;
        }

        BlockPos target = new BlockPos((int) p_26520_, (int) p_26521_, (int) p_26522_);
        Path path = this.createPath(ImmutableSet.of(target), 16, true, 1);

        if (path != null && this.moveTo(path, p_26523_)) {
            this.lastFailure = 0;
            this.pathfindFailures = 0;
            return true;
        } else {
            this.pathfindFailures++;
            this.lastFailure = currentTick;
            return false;
        }
    }

    // Paper start - optimise pathfinding
    private long pathfindFailures = 0;
    private long lastFailure = 0;
    // Paper end

    @Override
    public boolean moveTo(@NotNull Entity p_26532_, double p_26533_) {
        if(Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return false;
        long currentTick = this.level.getGameTime();
        // Paper start - Pathfinding optimizations
        if (this.pathfindFailures > 10 && this.path == null && currentTick < this.lastFailure + 40) {
            return false;
        }
        // Paper end
        Path path = this.createPath(p_26532_, 1);
        // Paper start - Pathfinding optimizations
        if (path != null && this.moveTo(path, p_26533_)) {
            this.lastFailure = 0;
            this.pathfindFailures = 0;
            return true;
        } else {
            this.pathfindFailures++;
            this.lastFailure = currentTick;
            return false;
        }
        // Paper end
    }

    @Override
    public boolean moveTo(@Nullable Path p_26537_, double p_26538_) {
        if(Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) return false;
        if (p_26537_ == null) {
            this.path = null;
            return false;
        }
        if (!p_26537_.sameAs(this.path)) {
            this.path = p_26537_;
        }

        if (this.isDone()) {
            return false;
        }

        boolean isProcessed = (this.path instanceof AsyncPath asyncPath && asyncPath.isProcessed()) || (!(this.path instanceof AsyncPath asyncPath));

        if (isProcessed) {
            this.trimPath();
            if(this.path.getNodeCount() <= 0) return false;
        }

        this.speedModifier = p_26538_;
        Vec3 vec3 = this.getTempMobPos();
        this.lastStuckCheck = this.tick;
        this.lastStuckCheckPos = vec3;

        return true;
    }


    protected void trimPath() {
        if (this.path == null) return;

        for (int i = 0; i < this.path.getNodeCount(); ++i) {
            Node node = this.path.getNode(i);
            Node node1 = i + 1 < this.path.getNodeCount() ? this.path.getNode(i + 1) : null;
            BlockState blockstate = this.level.getBlockState(new BlockPos(node.x, node.y, node.z));
            if (blockstate.is(BlockTags.CAULDRONS)) {
                this.path.replaceNode(i, node.cloneAndMove(node.x, node.y + 1, node.z));
                if (node1 != null && node.y >= node1.y) {
                    this.path.replaceNode(i + 1, node.cloneAndMove(node1.x, node.y + 1, node1.z));
                }
            }
        }
    }

    @Override
    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }

        if (this.path instanceof AsyncPath asyncPath && !asyncPath.isProcessed()) return;

        // Exact-arrival retry: we walked to the closest reachable point but did
        // not land on the target. Once that fallback path is finished (or we are
        // stuck on it), recompute toward the real target from where we now are.
        // A short cooldown prevents spamming the pathfinder every tick.
        if (this.awaitingExactRetry && this.targetPos != null) {
            boolean fallbackFinished = this.isDone() || this.isStuck();
            if (fallbackFinished && this.level.getGameTime() - this.lastExactRetry > 10L) {
                this.lastExactRetry = this.level.getGameTime();
                this.awaitingExactRetry = false; // re-armed by the next callback if still not exact
                this.path = this.createPath(this.targetPos, this.reachRange);
                return;
            }
        }

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3 vec3 = this.getTempMobPos();
                Vec3 vec31 = this.path.getNextEntityPos(this.mob);
                if (vec3.y > vec31.y && !this.mob.onGround() && Mth.floor(vec3.x) == Mth.floor(vec31.x) && Mth.floor(vec3.z) == Mth.floor(vec31.z)) {
                    this.path.advance();
                }
            }

            DebugPackets.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
            if (!this.isDone()) {
                Vec3 vec32 = this.path.getNextEntityPos(this.mob);
                this.mob.getMoveControl().setWantedPosition(vec32.x, this.getGroundY(vec32), vec32.z, this.speedModifier);
            }
        }
    }

    protected void followThePath() {
        if ((this.path instanceof AsyncPath asyncPath && !asyncPath.isProcessed())) return;
        Vec3 vec3 = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F ? this.mob.getBbWidth() / 2.0F : 0.75F - this.mob.getBbWidth() / 2.0F;
        Vec3i vec3i = this.path.getNextNodePos();
        double d0 = Math.abs(this.mob.getX() - ((double)vec3i.getX() + (this.mob.getBbWidth() + 1) / 2D)); //Forge: Fix MC-94054
        double d1 = Math.abs(this.mob.getY() - (double)vec3i.getY());
        double d2 = Math.abs(this.mob.getZ() - ((double)vec3i.getZ() + (this.mob.getBbWidth() + 1) / 2D)); //Forge: Fix MC-94054
        boolean flag = d0 <= (double)this.maxDistanceToWaypoint && d2 <= (double)this.maxDistanceToWaypoint && d1 < 1.0D; //Forge: Fix MC-94054
        if (flag || this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(vec3)) {
            this.path.advance();
        }

        this.doStuckDetection(vec3);
    }

    private boolean shouldTargetNextNodeInDirection(Vec3 p_26560_) {
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
            return false;
        } else {
            Vec3 vec3 = Vec3.atBottomCenterOf(this.path.getNextNodePos());
            if (!p_26560_.closerThan(vec3, 2.0D)) {
                return false;
            } else if (this.canMoveDirectly(p_26560_, this.path.getNextEntityPos(this.mob))) {
                return true;
            } else {
                Vec3 vec31 = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
                Vec3 vec32 = vec31.subtract(vec3);
                Vec3 vec33 = p_26560_.subtract(vec3);
                return vec32.dot(vec33) > 0.0D;
            }
        }
    }

    public void recomputePath() {
        if (this.level.getGameTime() - this.timeLastRecompute > 20L) {
            if (this.targetPos != null) {
                this.path = null;
                this.path = this.createPath(this.targetPos, this.reachRange);
                this.timeLastRecompute = this.level.getGameTime();
                this.hasDelayedRecomputation = false;
            }
        } else {
            this.hasDelayedRecomputation = true;
        }
    }

    private void resetStuckTimeout() {
        this.timeoutCachedNode = Vec3i.ZERO;
        this.timeoutTimer = 0L;
        this.timeoutLimit = 0.0D;
        this.isStuck = false;
    }

    @Override
    public boolean isStuck() {
        return this.isStuck;
    }
}