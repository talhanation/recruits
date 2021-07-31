package com.talhanation.recruits.entities;


import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RecruitEntity extends RecruitBaseEntity {


    public RecruitEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
        this.setItemInHand(Hand.MAIN_HAND, new ItemStack(Items.STONE_SWORD));
        //this.experienceValue = 6;
    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);

    }


    @Nullable
    public ILivingEntityData finalizeSpawn(@Nonnull IServerWorld world, @Nonnull DifficultyInstance difficulty, @Nonnull SpawnReason spawnReason, @Nullable ILivingEntityData livingdata, @Nullable CompoundNBT nbt) {
        livingdata = super.finalizeSpawn(world, difficulty, spawnReason, livingdata, nbt);
        //setCombatTask();
        return livingdata;
    }


    @Override
    public void setEquipment(){


    }
}
