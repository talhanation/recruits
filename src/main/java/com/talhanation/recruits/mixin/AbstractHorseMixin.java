package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends Animal {
    protected AbstractHorseMixin(EntityType<? extends Animal> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "isImmobile", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;isImmobile()Z"), cancellable = true)
    private void horseIsMobileWhenRecruitsRides(CallbackInfoReturnable<Boolean> callback) {
        if(getControllingPassenger() instanceof AbstractRecruitEntity){
            callback.setReturnValue(false);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "travel", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;travel(Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    private void superTravelWhenRecruitsRides(Vec3 vec3, CallbackInfo ci) {
        if (this.isAlive() && isVehicle() && getControllingPassenger() instanceof AbstractRecruitEntity) {
            super.travel(vec3);
            ci.cancel();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "positionRider", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V"), cancellable = true)
    private void superPositionRiderWhenRecruitsRides(Entity entity, CallbackInfo ci) {
        if (this.isAlive() && isVehicle() && getControllingPassenger() instanceof AbstractRecruitEntity) {
            super.positionRider(entity);
            ci.cancel();
        }
    }
}

