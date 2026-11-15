package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Camel.class)
public abstract class CamelMixin extends AbstractHorse {
    protected CamelMixin(EntityType<? extends AbstractHorse> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    // A camel refuses to move while it sits or changes its pose. Ridden by a recruit it stands up and moves
    // like a horse. Has to be canceled: otherwise the vanilla travel runs as well and the camel does two
    // movement steps per tick (twice the speed and acceleration, and it jerks while changing its pose).
    @SuppressWarnings("ConstantValue")
    @Inject(method = "travel", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/animal/camel/Camel;travel(Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    private void camelTravelSuperWhenRecruitsRides(Vec3 vec3, CallbackInfo callback) {
        if(getControllingPassenger() instanceof AbstractRecruitEntity){
            Camel camel = (Camel)(Object)this;
            if(!camel.level().isClientSide() && camel.isCamelSitting() && !camel.isInPoseTransition()) camel.standUp();

            super.travel(vec3);
            callback.cancel();
        }
    }

    // The camel AI is a brain, not goals - so unlike the goals of a horse it is not switched off by a mob
    // rider. Its random strolls (speed 2.0) and its panic (speed 4.0) took over the navigation the recruit
    // steers with. No brain while a recruit rides.
    @SuppressWarnings("ConstantValue")
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void noCamelBrainWhenRecruitsRides(CallbackInfo callback) {
        if(getControllingPassenger() instanceof AbstractRecruitEntity){
            callback.cancel();
        }
    }

}

