package com.talhanation.recruits.entities;

import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.function.Predicate;

public abstract class AbstractInventoryEntity extends TameableEntity {

    public final Inventory inventory = new Inventory(9);
    public final Inventory armor = new Inventory(4);
    public final Inventory hand = new Inventory(2);

    public AbstractInventoryEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void aiStep() {
        super.aiStep();
    }

    public void tick(){
        super.tick();
    }

    ////////////////////////////////////DATA////////////////////////////////////

    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        ListNBT list = new ListNBT();
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putByte("Slot", (byte) i);
                itemstack.save(compoundnbt);
                list.add(compoundnbt);
            }
        }
        ListNBT list1 = new ListNBT();
        for (int i = 0; i < this.hand.getContainerSize(); ++i) {
            ItemStack itemstack = this.hand.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putByte("Slot_Hand", (byte) i);
                itemstack.save(compoundnbt);
                list1.add(compoundnbt);
            }
        }
        ListNBT list2 = new ListNBT();
        for (int i = 0; i < this.armor.getContainerSize(); ++i) {
            ItemStack itemstack = this.armor.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putByte("Slot_Armor", (byte) i);
                itemstack.save(compoundnbt);
                list2.add(compoundnbt);
            }
        }

        nbt.put("Inventory", list);
        nbt.put("Hand", list1);
        nbt.put("Armor", list2);
    }

    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        ListNBT list = nbt.getList("Inventory", 30);
        for (int i = 0; i < list.size(); ++i) {
            CompoundNBT compoundnbt = list.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;

            this.inventory.setItem(j, ItemStack.of(compoundnbt));
        }

        ListNBT list1 = nbt.getList("Hand", 30);
        for (int i = 0; i < list1.size(); ++i) {
            CompoundNBT compoundnbt = list1.getCompound(i);
            int j = compoundnbt.getByte("Slot_Hand") & 255;

            this.hand.setItem(j, ItemStack.of(compoundnbt));
        }

        ListNBT list2 = nbt.getList("Armor", 30);
        for (int i = 0; i < list2.size(); ++i) {
            CompoundNBT compoundnbt = list2.getCompound(i);
            int j = compoundnbt.getByte("Slot_Armor") & 255;

            this.armor.setItem(j, ItemStack.of(compoundnbt));
        }

    }

    ////////////////////////////////////GET////////////////////////////////////

    public Inventory getInventory() {
        return this.inventory;
    }

    public Inventory getHand() {
        return this.hand;
    }

    public Inventory getArmor() {
        return this.armor;
    }

    ////////////////////////////////////SET////////////////////////////////////
/*
    public boolean setSlot(int slot, ItemStack itemStack) {
        if (super.setSlot(slot, itemStack)) {
            return true;
        } else {
            int i = slot - 300;
            if (i >= 0 && i < this.inventory.getContainerSize()) {
                this.inventory.setItem(i, itemStack);
                return true;
            } else {
                return false;
            }
        }
    }
*/
    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void die(DamageSource dmg) {
        super.die(dmg);
        for (int i = 0; i < this.inventory.getContainerSize(); i++)
            InventoryHelper.dropItemStack(this.level, getX(), getY(), getZ(), this.inventory.getItem(i));

        for (int i = 0; i < this.armor.getContainerSize(); i++)
            InventoryHelper.dropItemStack(this.level, getX(), getY(), getZ(), this.armor.getItem(i));

        for (int i = 0; i < this.hand.getContainerSize(); i++)
            InventoryHelper.dropItemStack(this.level, getX(), getY(), getZ(), this.hand.getItem(i));
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            Inventory inventory = this.getInventory();
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }

    @Override
    public abstract boolean wantsToPickUp(ItemStack itemStack);

    public abstract Predicate<ItemEntity> getAllowedItems();

    public abstract void openGUI(PlayerEntity player);

}