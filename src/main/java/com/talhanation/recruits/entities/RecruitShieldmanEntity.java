package com.talhanation.recruits.entities;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public class RecruitShieldmanEntity extends RecruitEntity{

    public RecruitShieldmanEntity(EntityType<? extends RecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    //ATTRIBUTES
    public static AttributeSupplier setAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .build();

    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(new TextComponent("Recruit"));
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.setGroup(1);
    }

    @Override
    public void setEquipment() {
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        inventory.setItem(12, new ItemStack(Items.LEATHER_CHESTPLATE));
        inventory.setItem(13, new ItemStack(Items.LEATHER_LEGGINGS));
        inventory.setItem(14, new ItemStack(Items.LEATHER_BOOTS));
        inventory.setItem(10, new ItemStack(Items.SHIELD));

        int i = this.random.nextInt(10);
        if (i == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
        } else
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
            inventory.setItem(9, new ItemStack(Items.STONE_AXE));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_241840_1_, AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    public int recruitCosts() {
        return 10;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
    }
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
    }
}
