package com.talhanation.recruits.entities;


import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class RecruitEntity extends AbstractRecruitEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public RecruitEntity(EntityType<? extends AbstractRecruitEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(new StringTextComponent("Recruit"));
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
        this.setItemSlot(EquipmentSlotType.HEAD, new ItemStack(Items.LEATHER_HELMET));
        this.setItemSlot(EquipmentSlotType.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlotType.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlotType.FEET, new ItemStack(Items.LEATHER_BOOTS));

        inventory.setItem(11, new ItemStack(Items.LEATHER_HELMET));
        inventory.setItem(12, new ItemStack(Items.LEATHER_CHESTPLATE));
        inventory.setItem(13, new ItemStack(Items.LEATHER_LEGGINGS));
        inventory.setItem(14, new ItemStack(Items.LEATHER_BOOTS));
        int i = this.random.nextInt(8);
        if (i == 0) {
            inventory.setItem(9, new ItemStack(Items.STONE_AXE));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_AXE));
        } else  if (i == 1){
            inventory.setItem(9, new ItemStack(Items.STONE_SWORD));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_SWORD));
        } else  if (i == 2){
            inventory.setItem(9, new ItemStack(Items.STONE_SWORD));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_SWORD));
        }else{
            inventory.setItem(9, new ItemStack(Items.WOODEN_SWORD));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        }
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
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










