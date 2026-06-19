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

        // Adaptive budget. Most paths (open terrain) hit the target exactly
        // within the cheap base budget and break out immediately. The expensive
        // full budget is only spent while the search keeps getting closer to the
        // target. An unreachable target (walled in, in lava, dead mob) stalls and
        // stops shortly after the base budget instead of grinding to 8x every
        // time - exactly the mass-battle spike case.
        int baseBudget = (int) ((float) this.maxVisitedNodes * p_164722_);
        int fullBudget = (int) (baseBudget * EXACT_SEARCH_BUDGET);

        // How many nodes we allow to pass WITHOUT getting closer before giving
        // up on the expensive phase. Scaled off the base budget so small/large
        // searches behave proportionally.
        int noProgressLimit = Math.max(64, baseBudget / 2);

        float bestHSeen = Float.MAX_VALUE;
        int nodesSinceImprovement = 0;

        // Track the closest node to the (single) target by TRUE distance, to use
        // as the fallback endpoint. Kept separate from Target.getBestNode(),
        // which is chosen by the Y-weighted heuristic and would bias the fallback
        // toward raised blocks on flat ground.
        Node closestTrueNode = p_164718_;
        float closestTrueDist = Float.MAX_VALUE;
        Target primaryTarget = p_164719_.get(0).getKey();
        Node reachedNode = null; // the node that actually reached the target

        while (!openSet.isEmpty()) {
            ++i;
            if (i >= fullBudget) {
                break;
            }

            Node node = openSet.pop();
            node.closed = true;

            if (visitedDebug != null) {
                visitedDebug.add(new com.talhanation.recruits.client.events.ClientPathDebug.Entry(
                        node.x, node.y, node.z, node.f, node.costMalus, false));
            }

            // EXACT hit: a node on the target block itself, OR standing directly
            // on top of it (target + 1 in Y, same X/Z). Move orders point at the
            // surface block, which is solid - you cannot stand INSIDE it, so the
            // standable spot is one above. Without this, ground targets never
            // register as reached and always fell through to the fallback (which
            // is Y-biased and would climb onto the nearest raised block).
            for (final Map.Entry<Target, BlockPos> entry : p_164719_) {
                Target target = entry.getKey();
                boolean sameColumn = node.x == target.x && node.z == target.z;
                if (sameColumn && (node.y == target.y || node.y == target.y + 1)) {
                    target.setReached();
                    reachedExactly.add(entry);
                    reachedNode = node; // the actual standable spot we hit
                }
            }

            if (!reachedExactly.isEmpty()) {
                break;
            }

            // Track progress toward the target (node.h holds the scaled heuristic).
            if (node.h < bestHSeen) {
                bestHSeen = node.h;
                nodesSinceImprovement = 0;
            } else {
                nodesSinceImprovement++;
            }

            // Track the truly-closest node (for an unbiased fallback endpoint).
            float td = trueDistance(node, primaryTarget);
            if (td < closestTrueDist) {
                closestTrueDist = td;
                closestTrueNode = node;
            }

            // Past the cheap base budget, bail out once we have gone a whole
            // no-progress window without getting any closer. Before the base
            // budget we always keep going (cheap, and usually finds the hit).
            if (i >= baseBudget && nodesSinceImprovement >= noProgressLimit) {
                break;
            }

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

        Path best;

        if (exact) {
            // Reached: build the path to the node that actually hit the target
            // (the standable spot), not the Y-weighted getBestNode.
            best = this.reconstructPath(reachedNode, p_164719_.get(0).getValue(), true);
        } else {
            // No exact hit within budget. Fall back to the node that is truly
            // closest to the target (tracked above by unweighted distance), and
            // flag the path NOT reached so the navigation retries from there.
            // Using true distance (not the Y-weighted heuristic / getBestNode)
            // stops the recruit from preferring to climb onto the nearest raised
            // block on flat ground.
            best = this.reconstructPath(closestTrueNode, p_164719_.get(0).getValue(), false);
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

    /** Plain Euclidean distance, no axis weighting (used for fallback ranking). */
    private float trueDistance(Node node, Target target) {
        float dx = (float) target.x - node.x;
        float dy = (float) target.y - node.y;
        float dz = (float) target.z - node.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}