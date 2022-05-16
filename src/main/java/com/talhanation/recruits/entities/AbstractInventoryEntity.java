package com.talhanation.recruits.entities;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.world.entity.EntityType;

public abstract class AbstractInventoryEntity extends TamableAnimal {


    //iv slots
    //9,10 = hand
    //11,12,13,14 = armor
    //0-8 = inv

    public SimpleContainer inventory;
    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;
    private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);

    public AbstractInventoryEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
        this.createInventory();
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
        if (!level.isClientSide) recDetectEquipmentUpdates();

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
        this.createInventory();

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundTag compoundnbt = listnbt.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;
            if (j < this.inventory.getContainerSize()) {
                this.inventory.setItem(j, ItemStack.of(compoundnbt));
            }
        }

    }

    ////////////////////////////////////GET////////////////////////////////////

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public int getInventorySize() {
        return 15;
    }

    public int getInventoryColumns() {
        return 3;
    }


    ////////////////////////////////////SET////////////////////////////////////

    @Override
    public boolean setSlot(int id, ItemStack itemStack) {
        super.setSlot(id, itemStack);
        return true;
    }


    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    protected void createInventory() {
        SimpleContainer inventory = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (inventory != null) {
            int i = Math.min(inventory.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = inventory.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }
        this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.inventory));
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
        for (int i = 0; i < this.inventory.getContainerSize(); i++)
            Containers.dropItemStack(this.level, getX(), getY(), getZ(), this.inventory.getItem(i));
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            SimpleContainer inventory = this.inventory;
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }
            //this.recDetectEquipmentUpdates();
            this.checkItemsInInv();
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
    protected void invalidateCaps() {
        super.invalidateCaps();
        if (itemHandler != null) {
            LazyOptional<?> oldHandler = itemHandler;
            itemHandler = null;
            oldHandler.invalidate();
        }
    }

    public void recDetectEquipmentUpdates() {
        Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }

    }

    private void handleHandSwap(Map<EquipmentSlot, ItemStack> p_241342_1_) {
        ItemStack itemstack = p_241342_1_.get(EquipmentSlot.MAINHAND);
        ItemStack itemstack1 = p_241342_1_.get(EquipmentSlot.OFFHAND);
        if (itemstack != null && itemstack1 != null && ItemStack.matches(itemstack, this.getLastHandItem(EquipmentSlot.OFFHAND)) && ItemStack.matches(itemstack1, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
            ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
            p_241342_1_.remove(EquipmentSlot.MAINHAND);
            p_241342_1_.remove(EquipmentSlot.OFFHAND);
            this.setLastHandItem(EquipmentSlot.MAINHAND, itemstack.copy());
            this.setLastHandItem(EquipmentSlot.OFFHAND, itemstack1.copy());
        }
    }

    @Nullable
    private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> map = null;

        for(EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
            ItemStack itemstack;
            switch(equipmentslottype.getType()) {
                case HAND:
                    itemstack = this.getLastHandItem(equipmentslottype);
                    break;
                case ARMOR:
                    itemstack = this.getLastArmorItem(equipmentslottype);
                    break;
                default:
                    continue;
            }

            ItemStack itemstack1 = this.getItemBySlot(equipmentslottype);
            if (!ItemStack.matches(itemstack1, itemstack)) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent(this, equipmentslottype, itemstack, itemstack1));
                if (map == null) {
                    map = Maps.newEnumMap(EquipmentSlot.class);
                }

                map.put(equipmentslottype, itemstack1);
                if (!itemstack.isEmpty()) {
                    this.getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(equipmentslottype));
                }

                if (!itemstack1.isEmpty()) {
                    this.getAttributes().addTransientAttributeModifiers(itemstack1.getAttributeModifiers(equipmentslottype));
                }
            }
        }

        return map;
    }

    private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> p_241344_1_) {
        List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(p_241344_1_.size());
        p_241344_1_.forEach((p_241341_2_, p_241341_3_) -> {
            ItemStack itemstack = p_241341_3_.copy();
            list.add(Pair.of(p_241341_2_, itemstack));
            switch(p_241341_2_.getType()) {
                case HAND:
                    this.setLastHandItem(p_241341_2_, itemstack);
                    break;
                case ARMOR:
                    this.setLastArmorItem(p_241341_2_, itemstack);
            }

        });
        ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), list));
    }

    private ItemStack getLastHandItem(EquipmentSlot p_241347_1_) {
        return this.lastHandItemStacks.get(p_241347_1_.getIndex());
    }

    private void setLastHandItem(EquipmentSlot p_241345_1_, ItemStack p_241345_2_) {
        this.lastHandItemStacks.set(p_241345_1_.getIndex(), p_241345_2_);
    }

    private ItemStack getLastArmorItem(EquipmentSlot p_241346_1_) {
        return this.lastArmorItemStacks.get(p_241346_1_.getIndex());
    }

    private void setLastArmorItem(EquipmentSlot p_241343_1_, ItemStack p_241343_2_) {
        this.lastArmorItemStacks.set(p_241343_1_.getIndex(), p_241343_2_);
    }
}