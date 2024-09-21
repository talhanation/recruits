package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

public class RecruitsHorsePathNavigation extends GroundPathNavigation {
    AbstractHorse horse;
    public RecruitsHorsePathNavigation(AbstractHorse horse, Level world) {
        super(horse, world);
        this.horse = horse;
        horse.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        horse.setPathfindingMalus(BlockPathTypes.TRAPDOOR, -1.0F);
        horse.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.DAMAGE_CACTUS, 32.0F);
        horse.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        horse.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        horse.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
        horse.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        horse.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new RecruitsPathNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);

        return new PathFinder(this.nodeEvaluator, range);
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        ((RecruitsPathNodeEvaluator) this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        return this.moveTo(this.createPath(x, y, z, 0), speed);
    }
}
