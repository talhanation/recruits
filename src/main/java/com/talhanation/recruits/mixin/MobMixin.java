package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.navigation.RecruitsHorsePathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "createNavigation", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/Mob;createNavigation(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/ai/navigation/PathNavigation;"), cancellable = true)
    private void createNavigation(Level world, CallbackInfoReturnable<PathNavigation> callback) {
        if(((Mob)(Object)this) instanceof AbstractHorse horse && horse.getFirstPassenger() instanceof AbstractRecruitEntity && !(((Mob)(Object)this) instanceof SkeletonHorse)){
            callback.setReturnValue(new RecruitsHorsePathNavigation(horse, ((Mob)(Object)this).getCommandSenderWorld()));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getMaxFallDistance", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/Mob;getMaxFallDistance()I"), cancellable = true)
    private void getMaxFallDistance(CallbackInfoReturnable<Integer> callback){
        if(((Mob)(Object)this) instanceof AbstractHorse horse && horse.getFirstPassenger() instanceof AbstractRecruitEntity && !(((Mob)(Object)this) instanceof SkeletonHorse)){
            callback.setReturnValue(1);
        }
    }
}
