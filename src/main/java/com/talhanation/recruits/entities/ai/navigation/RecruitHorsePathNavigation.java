package com.talhanation.recruits.entities.ai.navigation;

import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

public class RecruitHorsePathNavigation extends GroundPathNavigation {

    public RecruitHorsePathNavigation(AbstractHorse horse, Level world) {
        super(horse, world);
        horse.setPathfindingMalus(BlockPathTypes.WATER, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.TRAPDOOR, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.DAMAGE_CACTUS, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        horse.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        horse.setPathfindingMalus(BlockPathTypes.FENCE, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new RecruitsHorseWalkNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, range);
    }
}
