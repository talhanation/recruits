package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    /*
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "travelRidden", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/LivingEntity;travelRidden(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    private void TravelWhenRecruitsRides(LivingEntity entity, Vec3 vec3, CallbackInfo ci) {
        if (((LivingEntity)(Object)this) instanceof Saddleable && ((LivingEntity)(Object)this).isVehicle() && entity instanceof AbstractRecruitEntity) {
            ((LivingEntity)(Object)this).travel(vec3);
            ci.cancel();
        }
    }
    */
}
