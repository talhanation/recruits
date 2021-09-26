package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;


@Mixin({MobEntity.class})
public class MobEntityMixin extends MobEntity {

    protected MobEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At(value = "HEAD"), method = "getMoveControl", cancellable = true)
    public MovementController getMoveControl() {
        Entity entityControlling = this.getControllingPassenger();
        if (entityControlling instanceof AbstractRecruitEntity){
            AbstractRecruitEntity recruit = (AbstractRecruitEntity)entityControlling;

            if (this.getVehicle() instanceof MobEntity) {
                return recruit.getMoveControl();
            }
        }
        return super.getMoveControl();
    }
}
