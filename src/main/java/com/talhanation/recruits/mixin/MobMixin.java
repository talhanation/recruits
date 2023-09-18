package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.navigation.RecruitHorsePathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Mob.class)
public abstract class MobMixin{

    //An Failded attempt to fix horse pathfinder when recruit is mounted so the recruit does not die in 2 block high areas
    // If the pathfinder is applied the Horse is not moving at all
    /*
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getNavigation", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/Mob;getNavigation()Lnet/minecraft/world/entity/ai/navigation/PathNavigation"), cancellable = true)
    public void horseGetNavigationWhenRecruitsRides(CallbackInfoReturnable<PathNavigation> cir) {
        if (((Mob)(Object)this) instanceof AbstractHorse horse && horse.isAlive() && horse.isVehicle() && horse.getControllingPassenger() instanceof AbstractRecruitEntity) {
            cir.setReturnValue(new RecruitHorsePathNavigation(horse, horse.getLevel()));
        }
    }
    */

}
