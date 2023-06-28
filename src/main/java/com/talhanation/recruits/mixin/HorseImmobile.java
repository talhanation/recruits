package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;


@Mixin(AbstractHorse.class)
public abstract class HorseImmobile {

    @Shadow @Nullable public abstract LivingEntity getControllingPassenger();

    @SuppressWarnings("ConstantValue")
    @Inject(method = "isImmobile", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;isImmobile()Z"), cancellable = true)
    private void horseIsMobileWhenRecruitsRides(CallbackInfoReturnable<Boolean> callback) {
        if(getControllingPassenger() instanceof AbstractRecruitEntity){
            callback.setReturnValue(false);
        }
    }
}
