package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
    @Inject(method = "prepare", at = @At(value = "TAIL", target = "Lnet/minecraft/world/level/pathfinder/NodeEvaluator;prepare(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;)V"), cancellable = true)
    public void horseGetStartNodeWhenRecruitsRides(PathNavigationRegion region, Mob mob, CallbackInfo info){
        if(mob instanceof AbstractHorse horse && horse.isVehicle() && horse.getControllingPassenger() instanceof AbstractRecruitEntity recruit){
            this.entityHeight = Mth.floor(horse.getBbHeight() + 2.0F);

            horse.setPathfindingMalus(BlockPathTypes.WATER, 32.0F);
            horse.setPathfindingMalus(BlockPathTypes.TRAPDOOR, -1.0F);
            horse.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
            horse.setPathfindingMalus(BlockPathTypes.DAMAGE_CAUTIOUS, 32.0F);
            horse.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
            horse.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
            horse.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
            horse.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
            horse.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
        }
    }


}
