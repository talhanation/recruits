package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncPathfinder;
import com.talhanation.recruits.pathfinding.NodeEvaluatorGenerator;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class RecruitPathNavigation extends AsyncGroundPathNavigation {
    private static BiFunction<Integer, NodeEvaluator, PathFinder> pathfinderSupplier = (p_26453_, nodeEvaluator) -> new PathFinder(nodeEvaluator, p_26453_);
    AbstractRecruitEntity recruit;

    private static final NodeEvaluatorGenerator nodeEvaluatorGenerator = () -> {
        NodeEvaluator nodeEvaluator = new RecruitsPathNodeEvaluator();

        nodeEvaluator.setCanOpenDoors(true);
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanFloat(true);

        return nodeEvaluator;
    };

    public RecruitPathNavigation(AbstractRecruitEntity recruit, Level world) {
        super(recruit, world);
        this.recruit = recruit;
        if(RecruitsServerConfig.UseAsyncPathfinding.get()) {
            pathfinderSupplier = (p_26453_, nodeEvaluator) -> new AsyncPathfinder(nodeEvaluator, p_26453_, nodeEvaluatorGenerator, this.level);
        }
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new RecruitsPathNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);

        return pathfinderSupplier.apply(range, this.nodeEvaluator);
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        // Allow the pathfinder to descend up to the evaluator's safe-fall limit
        // (see RecruitsPathNodeEvaluator.MAX_SAFE_FALL). With the old value of 1
        // the vanilla open-block scan turned any drop > 1 into a BLOCKED node, so
        // recruits could never step down into a cave / lower terrain and instead
        // hugged the surface. The graded fall malus still discourages big drops.
        this.recruit.setMaxFallDistance(3);
        ((RecruitsPathNodeEvaluator) this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        // Delegate to the unified coordinate moveTo (now identical to the follow
        // path: accuracy 16, touch, failure throttling) instead of building a
        // separate lean path here.
        return super.moveTo(x, y, z, speed);
    }
}