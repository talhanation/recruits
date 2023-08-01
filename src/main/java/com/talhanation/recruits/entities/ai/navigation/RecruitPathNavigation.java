package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

public class RecruitPathNavigation extends GroundPathNavigation {

    public RecruitPathNavigation(AbstractRecruitEntity recruit, Level world) {
        super(recruit, world);
        recruit.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, range);
    }
}
