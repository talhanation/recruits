package com.talhanation.recruits.pathfinding;


import com.talhanation.recruits.util.ProcessState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * i'll be using this to represent a path that not be processed yet!
 */
public class AsyncPath extends Path {
    /**
     * marks whether this async path has been processed
     */
    private volatile ProcessState processState = ProcessState.WAITING;
    /**
     * runnables waiting for this to be processed
     */
    private final List<Runnable> postProcessing = new ArrayList<>(0);
    /**
     * a list of positions that this path could path towards
     */
    private final Set<BlockPos> positions;
    /**
     * the supplier of the real processed path
     */
    private final Supplier<Path> pathSupplier;
    /*
     * Processed values
     */
    /**
     * this is a reference to the nodes list in the parent `Path` object
     */
    private final List<Node> nodes;
    /**
     * the block we're trying to path to
     * <p>
     * while processing, we have no idea where this is so consumers of `Path` should check that the path is processed before checking the target block
     */
    private @Nullable BlockPos target;
    /**
     * how far we are to the target
     * <p>
     * while processing, the target could be anywhere but theoretically we're always "close" to a theoretical target so default is 0
     */
    private float distToTarget = 0;
    /**
     * whether we can reach the target
     * <p>
     * while processing we can always theoretically reach the target so default is true
     */
    private boolean canReach = true;
    private Level level;

    public AsyncPath(@NotNull List<Node> emptyNodeList, @NotNull Set<BlockPos> positions, @NotNull Level level, @NotNull Supplier<Path> pathSupplier) {
        //noinspection ConstantConditions
        super(emptyNodeList, null, false);

        this.nodes = emptyNodeList;
        this.positions = positions;
        this.pathSupplier = pathSupplier;
        this.level = level;

        AsyncPathProcessor.queue(this);
    }


    public boolean isProcessed() {
        return this.processState == ProcessState.COMPLETED;
    }

    /**
     * returns the future representing the processing state of this path
     *
     * @return a future
     */
    public synchronized void postProcessing(@NotNull Runnable runnable) {
        if (isProcessed()) {
            runnable.run();
        } else {
            this.postProcessing.add(runnable);
        }
    }

    /**
     * an easy way to check if this processing path is the same as an attempted new path
     *
     * @param positions - the positions to compare against
     * @return true if we are processing the same positions
     */
    public boolean hasSameProcessingPositions(final Set<BlockPos> positions) {
        if (this.positions.size() != positions.size()) {
            return false;
        }

        return this.positions.containsAll(positions);
    }

    /**
     * starts processing this path
     */
    public synchronized void process() {
        if (this.processState == ProcessState.COMPLETED || this.processState == ProcessState.PROCESSING) {
            return;
        }

        processState = ProcessState.PROCESSING;

        final Path bestPath = this.pathSupplier.get();

        if (bestPath != null) {
            this.nodes.addAll(bestPath.nodes);
            this.target = bestPath.getTarget();
            this.distToTarget = bestPath.getDistToTarget();
            this.canReach = bestPath.canReach();
        } else {
            this.canReach = false;
        }

        processState = ProcessState.COMPLETED;

        List<Runnable> callbacks = new ArrayList<>(this.postProcessing);
        this.postProcessing.clear();
        for (Runnable runnable : callbacks) {
            try {
                runnable.run();
            } catch (Exception e) {
                com.talhanation.recruits.Main.LOGGER.error("Exception in AsyncPath post-processing callback", e);
            }
        }
    }

    /**
     * @return true wenn der Pfad vollständig verarbeitet ist und gelesen werden darf
     */
    private boolean ensureProcessed() {
        return this.processState == ProcessState.COMPLETED;
    }

    /*
     * overrides we need for final fields that we cannot modify after processing
     */
    @Override
    public @NotNull BlockPos getTarget() {
        if (!ensureProcessed()) return BlockPos.ZERO;
        return Objects.requireNonNull(this.target);
    }

    @Override
    public float getDistToTarget() {
        if (!ensureProcessed()) return Float.MAX_VALUE;
        return this.distToTarget;
    }

    @Override
    public boolean canReach() {
        if (!ensureProcessed()) return false;
        return this.canReach;
    }

    /*
     * overrides to ensure we're processed first
     */
    @Override
    public boolean isDone() {
        return this.isProcessed() && super.isDone();
    }

    @Override
    public void advance() {
        if (!ensureProcessed()) return;
        super.advance();
    }

    @Override
    public boolean notStarted() {
        if (!ensureProcessed()) return true;
        return super.notStarted();
    }

    @Nullable
    @Override
    public Node getEndNode() {
        if (!ensureProcessed()) return null;
        return super.getEndNode();
    }

    @Override
    public @NotNull Node getNode(int index) {
        if (!ensureProcessed()) throw new IllegalStateException("AsyncPath not yet processed");
        return super.getNode(index);
    }

    @Override
    public void truncateNodes(int length) {
        if (!ensureProcessed()) return;
        super.truncateNodes(length);
    }

    @Override
    public void replaceNode(int index, @NotNull Node node) {
        if (!ensureProcessed()) return;
        super.replaceNode(index, node);
    }

    @Override
    public int getNodeCount() {
        if (!ensureProcessed()) return 0;
        return super.getNodeCount();
    }

    @Override
    public int getNextNodeIndex() {
        if (!ensureProcessed()) return 0;
        return super.getNextNodeIndex();
    }

    @Override
    public void setNextNodeIndex(int nodeIndex) {
        if (!ensureProcessed()) return;
        super.setNextNodeIndex(nodeIndex);
    }

    @Override
    public @NotNull Vec3 getEntityPosAtNode(@NotNull Entity entity, int index) {
        if (!ensureProcessed()) return entity.position();
        return super.getEntityPosAtNode(entity, index);
    }

    @Override
    public @NotNull BlockPos getNodePos(int index) {
        if (!ensureProcessed()) return BlockPos.ZERO;
        return super.getNodePos(index);
    }

    @Override
    public @NotNull Vec3 getNextEntityPos(@NotNull Entity entity) {
        if (!ensureProcessed()) return entity.position();
        return super.getNextEntityPos(entity);
    }

    @Override
    public @NotNull BlockPos getNextNodePos() {
        if (!ensureProcessed()) return BlockPos.ZERO;
        return super.getNextNodePos();
    }

    @Override
    public @NotNull Node getNextNode() {
        if (!ensureProcessed()) throw new IllegalStateException("AsyncPath not yet processed");
        return super.getNextNode();
    }

    @Nullable
    @Override
    public Node getPreviousNode() {
        if (!ensureProcessed()) return null;
        return super.getPreviousNode();
    }
}