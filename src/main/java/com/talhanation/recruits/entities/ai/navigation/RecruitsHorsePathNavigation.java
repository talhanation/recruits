package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncPathfinder;
import com.talhanation.recruits.pathfinding.NodeEvaluatorGenerator;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

public class RecruitsHorsePathNavigation extends AsyncGroundPathNavigation {
    AbstractHorse horse;

    private static final NodeEvaluatorGenerator nodeEvaluatorGenerator = () -> {
        NodeEvaluator nodeEvaluator = new RecruitsPathNodeEvaluator();

        nodeEvaluator.setCanOpenDoors(true);
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanFloat(true);

        return nodeEvaluator;
    };

    public RecruitsHorsePathNavigation(AbstractHorse horse, Level world) {
        super(horse, world);
        this.horse = horse;
    }

    protected @NotNull AsyncPathfinder createPathFinder(int range) {
        this.nodeEvaluator = new RecruitsPathNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);

        return new AsyncPathfinder(this.nodeEvaluator, range, nodeEvaluatorGenerator, this.level);
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        ((RecruitsPathNodeEvaluator) this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        return this.moveTo(this.createPathAsync(x, y, z, 0), speed);
    }
}
