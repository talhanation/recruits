package com.talhanation.recruits.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.world.World;

public class RecruitEntity extends RecruitBaseEntity {


    public <T extends Entity> RecruitEntity(EntityType<T> tEntityType, World world) {
    }


    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return super.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);

    }

}
