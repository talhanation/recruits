package com.talhanation.recruits.entities;


import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.function.Predicate;

import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RecruitEntity extends AbstractRecruitEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public RecruitEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

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
    public void setEquipment() {// doppelt weil bug
        // wenn nur setItemSlot = dann geht beim gui opening weg
        // wenn nur setItem = dann geht beim gui opening rein
        // wtf
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));

        inventory.setItem(11, new ItemStack(Items.LEATHER_HELMET));
        inventory.setItem(12, new ItemStack(Items.LEATHER_CHESTPLATE));
        inventory.setItem(13, new ItemStack(Items.LEATHER_LEGGINGS));
        inventory.setItem(14, new ItemStack(Items.LEATHER_BOOTS));
        int i = this.random.nextInt(8);
        if (i == 0) {
            inventory.setItem(9, new ItemStack(Items.STONE_AXE));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        } else  if (i == 1){
            inventory.setItem(9, new ItemStack(Items.STONE_SWORD));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        } else  if (i == 2){
            inventory.setItem(9, new ItemStack(Items.STONE_SWORD));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        }else{
            inventory.setItem(9, new ItemStack(Items.WOODEN_SWORD));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        }
    }

    @Nullable
    @Override
    public AgableMob getBreedOffspring(ServerLevel p_241840_1_, AgableMob p_241840_2_) {
        return null;
    }

    @Override
    public int recruitCosts() {
        return 3;
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return itemStack.isEdible();
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }
}










