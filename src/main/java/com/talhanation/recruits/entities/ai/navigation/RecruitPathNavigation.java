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
        recruit.setPathfindingMalus(BlockPathTypes.TRAPDOOR, 32.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DAMAGE_CACTUS, 32.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        recruit.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        recruit.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, range);
    }

    public boolean moveTo(double p_26520_, double p_26521_, double p_26522_, double p_26523_) {
        this.recruit.setMaxFallDistance(1);
        return this.moveTo(this.createPath(p_26520_, p_26521_, p_26522_, 1), p_26523_);
    }
}
