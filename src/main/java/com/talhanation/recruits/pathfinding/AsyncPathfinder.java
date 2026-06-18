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

    /**
     * Vertical weighting for the heuristic. Vertical separation is treated as
     * this many times more "distant" than horizontal, so the search commits to
     * routes that converge on the target's Y (descending into a cave, taking
     * stairs) instead of stalling at a horizontally-close but wrong-height spot.
     */
    private static final float Y_WEIGHT = 3.0F;

    /**
     * Budget multiplier for the exact search. We are async, so we can afford to
     * explore a lot more before giving up on an exact hit. Applied on top of the
     * per-call maxVisitedNodesMultiplier.
     */
    private static final float EXACT_SEARCH_BUDGET = 8.0F;

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
        var nodeEvaluator = this.nodeEvaluatorGenerator == null ? this.nodeEvaluator : NodeEvaluatorCache.takeNodeEvaluator(this.nodeEvaluatorGenerator);
        nodeEvaluator.prepare(p_77428_, p_77429_);
        Node node = nodeEvaluator.getStart();

        if (node == null) {
            if (this.nodeEvaluatorGenerator != null) {
                NodeEvaluatorCache.returnNodeEvaluator(nodeEvaluator);
            }
            return null;
        } else {
            List<Map.Entry<Target, BlockPos>> map = Lists.newArrayList();
            for (BlockPos pos : p_77430_) {
                map.add(new java.util.AbstractMap.SimpleEntry<>(nodeEvaluator.getGoal(pos.getX(), pos.getY(), pos.getZ()), pos));
            }

            if (this.nodeEvaluatorGenerator == null) {
                Main.LOGGER.error("No node evaluator generator present for Mob {}", p_77429_);
                return null;
            }
            final int mobId = p_77429_.getId();
            return new AsyncPath(Lists.newArrayList(), p_77430_, this.level, () -> {
                try {
                    return this.processPath(nodeEvaluator, node, map, p_77431_, p_77432_, p_77433_, mobId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    nodeEvaluator.done();
                    NodeEvaluatorCache.returnNodeEvaluator(nodeEvaluator);
                }
            });
        }
    }

    private @NotNull Path processPath(NodeEvaluator p_164717_, Node p_164718_, List<Map.Entry<Target, BlockPos>> p_164719_, float p_164720_, int p_164721_, float p_164722_, int mobId) {
        org.apache.commons.lang3.Validate.isTrue(!p_164719_.isEmpty());

        p_164718_.g = 0.0F;
        p_164718_.h = this.getBestH(p_164718_, p_164719_);
        p_164718_.f = p_164718_.h;

        BinaryHeap openSet = new BinaryHeap();
        openSet.insert(p_164718_);

        Node[] neighbors = new Node[32];

        // client-only debug capture (no netcode); null when disabled
        List<com.talhanation.recruits.client.events.ClientPathDebug.Entry> visitedDebug =
                com.talhanation.recruits.client.events.ClientPathDebug.isEnabled() ? new ArrayList<>() : null;

        int i = 0;
        List<Map.Entry<Target, BlockPos>> reachedExactly = Lists.newArrayListWithExpectedSize(p_164719_.size());

        // Generous budget: keep exploring for an EXACT hit before settling for a
        // partial path. We are async so this extra work is acceptable.
        int budget = (int) ((float) this.maxVisitedNodes * p_164722_ * EXACT_SEARCH_BUDGET);

        while (!openSet.isEmpty()) {
            ++i;
            if (i >= budget) {
                break;
            }

            Node node = openSet.pop();
            node.closed = true;

            if (visitedDebug != null) {
                visitedDebug.add(new com.talhanation.recruits.client.events.ClientPathDebug.Entry(
                        node.x, node.y, node.z, node.f, node.costMalus, false));
            }

            // EXACT hit only: a node that sits on the target block itself.
            for (final Map.Entry<Target, BlockPos> entry : p_164719_) {
                Target target = entry.getKey();
                if (node.x == target.x && node.y == target.y && node.z == target.z) {
                    target.setReached();
                    reachedExactly.add(entry);
                }
            }

            if (!reachedExactly.isEmpty()) {
                break;
            }

            // NOTE: no early "distanceTo(start) >= followRange" cutoff here. The
            // old guard stopped expansion at the follow range and was a reason
            // long ways around were never found. The budget above bounds us.
            int k = p_164717_.getNeighbors(neighbors, node);

            for (int l = 0; l < k; ++l) {
                Node node1 = neighbors[l];
                float f = this.distance(node, node1);
                node1.walkedDistance = node.walkedDistance + f;
                float f1 = node.g + f + node1.costMalus;
                if (!node1.inOpenSet() || f1 < node1.g) {
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

        boolean exact = !reachedExactly.isEmpty();

        Path best = null;
        float bestScore = Float.MAX_VALUE;

        if (exact) {
            // We have at least one path that ends exactly on a target. Pick the
            // cheapest by walked distance; reached = true.
            for (Map.Entry<Target, BlockPos> entry : reachedExactly) {
                Node end = entry.getKey().getBestNode();
                Path path = this.reconstructPath(end, entry.getValue(), true);
                float score = end.g + path.getNodeCount();
                if (best == null || score < bestScore) {
                    best = path;
                    bestScore = score;
                }
            }
        } else {
            // No exact hit within budget. Fall back to the closest reachable
            // node (Y-weighted), but flag the path NOT reached so the navigation
            // knows to retry from there toward the real target.
            for (Map.Entry<Target, BlockPos> entry : p_164719_) {
                Target target = entry.getKey();
                Node bestNode = target.getBestNode();
                Path path = this.reconstructPath(bestNode, entry.getValue(), false);
                float endDist = weightedDistance(bestNode, target);
                float score = endDist * 1000.0F + path.getNodeCount();
                if (best == null || score < bestScore) {
                    best = path;
                    bestScore = score;
                }
            }
        }

        // publish for the client-side overlay (no-op if disabled)
        if (visitedDebug != null && best != null) {
            List<com.talhanation.recruits.client.events.ClientPathDebug.Entry> pathNodes = new ArrayList<>();
            for (int n = 0; n < best.getNodeCount(); n++) {
                Node pn = best.getNode(n);
                pathNodes.add(new com.talhanation.recruits.client.events.ClientPathDebug.Entry(
                        pn.x, pn.y, pn.z, pn.f, pn.costMalus, true));
            }
            BlockPos chosenTarget = best.getTarget();
            com.talhanation.recruits.client.events.ClientPathDebug.publish(
                    mobId, chosenTarget, pathNodes, visitedDebug, !exact);
        }

        return best;
    }

    private Path reconstructPath(Node p_77435_, BlockPos p_77436_, boolean p_77437_) {
        List<Node> list = Lists.newArrayList();
        Node node = p_77435_;
        list.add(0, p_77435_);

        while (node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, p_77436_, p_77437_);
    }

    private float getBestH(Node p_164718_, List<Map.Entry<Target, BlockPos>> p_164719_) {
        float f = Float.MAX_VALUE;

        for (Map.Entry<Target, BlockPos> targetBlockPosEntry : p_164719_) {
            final Target target = targetBlockPosEntry.getKey();
            // Y-weighted heuristic instead of Node#distanceTo, applied
            // consistently for both the start node and every expanded node.
            float f1 = weightedDistance(p_164718_, target);
            target.updateBest(f1, p_164718_);
            f = Math.min(f1, f);
        }

        return f;
    }

    /** Euclidean distance with the Y axis weighted up by {@link #Y_WEIGHT}. */
    private float weightedDistance(Node node, Target target) {
        float dx = (float) target.x - node.x;
        float dy = ((float) target.y - node.y) * Y_WEIGHT;
        float dz = (float) target.z - node.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}