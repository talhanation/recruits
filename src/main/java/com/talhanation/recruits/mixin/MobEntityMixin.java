package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;


@Mixin({MobEntity.class})
public class MobEntityMixin extends MobEntity {


    protected MobEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    //inject
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
