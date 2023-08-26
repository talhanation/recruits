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

    @SuppressWarnings("ConstantValue")
    @Inject(method = "travel", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/animal/camel/Camel;travel(Lnet/minecraft/world/phys/Vec3;)V"))
    private void camelTravelSuperWhenRecruitsRides(Vec3 vec3, CallbackInfo callback) {
        if(getControllingPassenger() instanceof AbstractRecruitEntity){
            super.travel(vec3);
        }
    }

}

