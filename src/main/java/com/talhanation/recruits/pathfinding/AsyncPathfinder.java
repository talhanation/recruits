package com.talhanation.recruits.pathfinding;

import com.google.common.collect.Lists;
import com.talhanation.recruits.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class AsyncPathfinder extends PathFinder {
    private final NodeEvaluator nodeEvaluator;
    private final int maxVisitedNodes;
    private Level level;

    private NodeEvaluatorGenerator nodeEvaluatorGenerator;

    public AsyncPathfinder(NodeEvaluator p_77425_, int p_77426_) {
        super(p_77425_, p_77426_);
        this.nodeEvaluator = p_77425_;
        this.maxVisitedNodes = p_77426_;
    }

    public AsyncPathfinder(NodeEvaluator p_77425_, int p_77426_, Level level) {
        super(p_77425_, p_77426_);
        this.nodeEvaluator = p_77425_;
        this.maxVisitedNodes = p_77426_;
        this.level = level;
    }

    public AsyncPathfinder(NodeEvaluator p_77425_, int p_77426_, NodeEvaluatorGenerator nodeEvaluatorGenerator, Level level) {
        super(p_77425_, p_77426_);
        this.maxVisitedNodes = p_77426_;
        this.nodeEvaluatorGenerator = nodeEvaluatorGenerator;
        this.nodeEvaluator = p_77425_;
        this.level = level;
    }

    @Nullable
    public Path findPath(@NotNull PathNavigationRegion p_77428_, @NotNull Mob p_77429_, @NotNull Set<BlockPos> p_77430_, float p_77431_, int p_77432_, float p_77433_) {
        //this.openSet.clear(); // petal - it's always cleared in processPath
        // petal start - use a generated evaluator if we have one otherwise run sync
        var nodeEvaluator = this.nodeEvaluatorGenerator == null ? this.nodeEvaluator : NodeEvaluatorCache.takeNodeEvaluator(this.nodeEvaluatorGenerator);
        nodeEvaluator.prepare(p_77428_, p_77429_);
        Node node = nodeEvaluator.getStart();

        if (node == null) {
            if (this.nodeEvaluatorGenerator != null) {
                NodeEvaluatorCache.returnNodeEvaluator(nodeEvaluator);
            }

            return null;
        } else {
            // Paper start - remove streams - and optimize collection
            List<Map.Entry<Target, BlockPos>> map = Lists.newArrayList();
            for (BlockPos pos : p_77430_) {
                map.add(new java.util.AbstractMap.SimpleEntry<>(nodeEvaluator.getGoal(pos.getX(), pos.getY(), pos.getZ()), pos));
            }
            // Paper end
            // petal start
            // Nope, no sync pathfinding
            // Either patch mob to add NodeEvaluatorGenerator or go out
            if (this.nodeEvaluatorGenerator == null) {
                Main.LOGGER.error("No node evaluator generator present for Mob {}", p_77429_);
                return null;
            }
            return new AsyncPath(Lists.newArrayList(), p_77430_, this.level, () -> {
                try {
                    return this.processPath(nodeEvaluator, node, map, p_77431_, p_77432_, p_77433_);
                }  catch (Exception e) {
                        e.printStackTrace();
                        return null;
                } finally {
                    nodeEvaluator.done();
                    NodeEvaluatorCache.returnNodeEvaluator(nodeEvaluator);
                }
            });
            // petal end
        }
    }

    private @NotNull Path processPath(NodeEvaluator p_164717_, Node p_164718_, List<Map.Entry<Target, BlockPos>> p_164719_, float p_164720_, int p_164721_, float p_164722_) { // petal - sync to only use the caching functions in this class on a single thread
        org.apache.commons.lang3.Validate.isTrue(!p_164719_.isEmpty()); // ensure that we have at least one position, which means we'll always return a path
        // Set<Target> set = p_164719_.keySet();

        p_164718_.g = 0.0F;
        p_164718_.h = this.getBestH(p_164718_, p_164719_);
        p_164718_.f = p_164718_.h;

        BinaryHeap openSet = new BinaryHeap();
        openSet.insert(p_164718_);

        Node[] neighbors = new Node[32];

        int i = 0;
        List<Map.Entry<Target, BlockPos>> entryList = Lists.newArrayListWithExpectedSize(p_164719_.size()); // Paper - optimize collection
        int j = (int) ((float) this.maxVisitedNodes * p_164722_);

        while (!openSet.isEmpty()) {
            ++i;
            if (i >= j) {
                break;
            }

            Node node = openSet.pop();
            node.closed = true;

            // Paper start - optimize collection
            for (final Map.Entry<Target, BlockPos> entry : p_164719_) {
                Target target = entry.getKey();
                if (node.distanceManhattan(target) <= p_164721_) {
                    target.setReached();
                    entryList.add(entry);
                    // Paper end - Perf: remove streams and optimize collection
                }
            }

            if (!entryList.isEmpty()) {
                break;
            }

            if (!(node.distanceTo(p_164718_) >= p_164720_)) {
                int k = p_164717_.getNeighbors(neighbors, node);

                for (int l = 0; l < k; ++l) {
                    Node node1 = neighbors[l];
                    float f = this.distance(node, node1);
                    node1.walkedDistance = node.walkedDistance + f;
                    float f1 = node.g + f + node1.costMalus;
                    if (node1.walkedDistance < p_164720_ && (!node1.inOpenSet() || f1 < node1.g)) {
                        node1.cameFrom = node;
                        node1.g = f1;
                        node1.h = this.getBestH(node1, p_164719_) * 1.5F;
                        if (node1.inOpenSet()) {
                            openSet.changeCost(node1, node1.g + node1.h);
                        } else {
                            node1.f = node1.g + node1.h;
                            openSet.insert(node1);
                        }
                    }
                }
            }
        }

        Path best = null;
        boolean entryListIsEmpty = entryList.isEmpty();

        Comparator<Path> comparator = entryListIsEmpty
                ? Comparator.comparingInt(Path::getNodeCount)
                : Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount);

        for (Map.Entry<Target, BlockPos> entry : entryListIsEmpty ? p_164719_ : entryList) {
            Path path = this.reconstructPath(entry.getKey().getBestNode(), entry.getValue(), !entryListIsEmpty);
            if (best == null || comparator.compare(path, best) < 0) {
                best = path;
            }
        }

        // petal - ignore this warning, we know that the above loop always runs at least once since positions is not empty
        //noinspection ConstantConditions
        return best;
        // Paper end
        // petal end
    }

    private Path reconstructPath(Node p_77435_, BlockPos p_77436_, boolean p_77437_) {
        List<Node> list = Lists.newArrayList();
        Node node = p_77435_;
        list.add(0, p_77435_);

        while(node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, p_77436_, p_77437_);
    }

    private float getBestH(Node p_164718_, List<Map.Entry<Target, BlockPos>> p_164719_) { // Paper - Perf: remove streams and optimize collection; Set<Target> -> List<Map.Entry<Target, BlockPos>>
        float f = Float.MAX_VALUE;

        // Paper start - Perf: remove streams and optimize collection
        for (Map.Entry<Target, BlockPos> targetBlockPosEntry : p_164719_) {
            final Target target = targetBlockPosEntry.getKey();
            // Paper end - Perf: remove streams and optimize collection
            float f1 = p_164718_.distanceTo(target);
            target.updateBest(f1, p_164718_);
            f = Math.min(f1, f);
        }

        return f;
    }
}
