package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

public class RecruitPathNavigation extends GroundPathNavigation {
    AbstractRecruitEntity recruit;
    public RecruitPathNavigation(AbstractRecruitEntity recruit, Level world) {
        super(recruit, world);
        this.recruit = recruit;
        recruit.setPathfindingMalus(BlockPathTypes.WATER, 32.0F);
        recruit.setPathfindingMalus(BlockPathTypes.TRAPDOOR, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DAMAGE_CAUTIOUS, 32.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        recruit.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new RecruitsPathNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, range);
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        this.recruit.setMaxFallDistance(1);
        ((RecruitsPathNodeEvaluator) this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        return this.moveTo(this.createPath(x, y, z, 0), speed);
    }
}
