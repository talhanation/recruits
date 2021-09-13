package com.talhanation.recruits.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({AbstractHorseEntity.class})
public class AbstractHorseEntityMixin extends AbstractHorseEntity{

    protected AbstractHorseEntityMixin(EntityType<? extends AbstractHorseEntity> p_i48563_1_, World p_i48563_2_) {
        super(p_i48563_1_, p_i48563_2_);
    }

    public AbstractRecruitEntity getControllingRecruit(){
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof AbstractRecruitEntity) {
                return (AbstractRecruitEntity) passenger;
            }
        }
        return null;
    }
}
