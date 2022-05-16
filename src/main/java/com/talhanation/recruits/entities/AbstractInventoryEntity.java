package com.talhanation.recruits.entities;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class AbstractInventoryEntity extends TamableAnimal {

    //iv slots
    //9,10 = hand
    //11,12,13,14 = armor
    //0-8 = inv

    public Inventory inventory;
    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;
    private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);

    public AbstractInventoryEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
        //if (!level.isClientSide) recDetectEquipmentUpdates();

        //updateRecruitInvSlots(); //funkt nicht
    }

    ////////////////////////////////////DATA////////////////////////////////////
     // damit er automatisch immer das setzt was in dem equp hat
    // jaaa eine for schleife wäre besser ... aber so sieht man welcher slot was sein soll
    /*
    public void updateRecruitInvSlots(){
        ItemStack itemStack = getItemBySlot(EquipmentSlotType.HEAD);
        setSlot(11, itemStack);

        ItemStack itemStack2 = getItemBySlot(EquipmentSlotType.CHEST);
        setSlot(12, itemStack2);

        ItemStack itemStack3 = getItemBySlot(EquipmentSlotType.LEGS);
        setSlot(13, itemStack3);

        ItemStack itemStack4 = getItemBySlot(EquipmentSlotType.FEET);
        setSlot(14, itemStack4);

        ItemStack itemStack5 = getItemBySlot(EquipmentSlotType.MAINHAND);
        setSlot(9, itemStack5);

        ItemStack itemStack6 = getItemBySlot(EquipmentSlotType.OFFHAND);
        setSlot(10, itemStack6);
    }

     */


    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ListTag listnbt = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("Slot", (byte) i);
                itemstack.save(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }

        nbt.put("Items", listnbt);
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag listnbt = nbt.getList("Items", 10);//muss 10 sen amk sonst nix save

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundTag compoundnbt = listnbt.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;
            if (j < this.inventory.getContainerSize()) {
                this.inventory.setItem(j, ItemStack.of(compoundnbt));
            }
        }

    }

    ////////////////////////////////////GET////////////////////////////////////

    public Inventory getInventory() {
        return this.inventory;
    }

    public int getInventorySize() {
        return 15;
    }

    public int getInventoryColumns() {
        return 3;
    }


    ////////////////////////////////////SET////////////////////////////////////

    /*@Override
    public boolean setSlot(int id, ItemStack itemStack) {
        super.setSlot(id, itemStack);
        return true;
    }
     */

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////



    public void die(DamageSource dmg) {
        super.die(dmg);
        for (int i = 0; i < this.inventory.getContainerSize(); i++)
            inventory.dropAll();
    }



    public abstract void checkItemsInInv();

    @Override
    public abstract boolean wantsToPickUp(ItemStack itemStack);

    public abstract Predicate<ItemEntity> getAllowedItems();

    public abstract void openGUI(Player player);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (this.isAlive() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && itemHandler != null)
            return itemHandler.cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemHandler != null) {
            LazyOptional<?> oldHandler = itemHandler;
            itemHandler = null;
            oldHandler.invalidate();
        }
    }

}