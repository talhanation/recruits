package com.talhanation.recruits.entities;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractInventoryEntity extends TameableEntity {


    //iv slots
    //9,10 = hand
    //11,12,13,14 = armor
    //0-8 = inv

    public Inventory inventory;
    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;
    private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);

    public AbstractInventoryEntity(EntityType<? extends TameableEntity> entityType, World world) {
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

    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        ListNBT listnbt = new ListNBT();
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putByte("Slot", (byte) i);
                itemstack.save(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }

        nbt.put("Items", listnbt);
    }

    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        ListNBT listnbt = nbt.getList("Items", 10);//muss 10 sen amk sonst nix save
        this.createInventory();

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundNBT compoundnbt = listnbt.getCompound(i);
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

    @Override
    public boolean setSlot(int id, ItemStack itemStack) {
        super.setSlot(id, itemStack);
        return true;
    }


    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    protected void createInventory() {
        Inventory inventory = this.inventory;
        this.inventory = new Inventory(this.getInventorySize());
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
            InventoryHelper.dropItemStack(this.level, getX(), getY(), getZ(), this.inventory.getItem(i));
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            Inventory inventory = this.inventory;
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

    public abstract void openGUI(PlayerEntity player);

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
        Map<EquipmentSlotType, ItemStack> map = this.collectEquipmentChanges();
        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }

    }

    private void handleHandSwap(Map<EquipmentSlotType, ItemStack> p_241342_1_) {
        ItemStack itemstack = p_241342_1_.get(EquipmentSlotType.MAINHAND);
        ItemStack itemstack1 = p_241342_1_.get(EquipmentSlotType.OFFHAND);
        if (itemstack != null && itemstack1 != null && ItemStack.matches(itemstack, this.getLastHandItem(EquipmentSlotType.OFFHAND)) && ItemStack.matches(itemstack1, this.getLastHandItem(EquipmentSlotType.MAINHAND))) {
            ((ServerWorld)this.level).getChunkSource().broadcast(this, new SEntityStatusPacket(this, (byte)55));
            p_241342_1_.remove(EquipmentSlotType.MAINHAND);
            p_241342_1_.remove(EquipmentSlotType.OFFHAND);
            this.setLastHandItem(EquipmentSlotType.MAINHAND, itemstack.copy());
            this.setLastHandItem(EquipmentSlotType.OFFHAND, itemstack1.copy());
        }
    }

    @Nullable
    private Map<EquipmentSlotType, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlotType, ItemStack> map = null;

        for(EquipmentSlotType equipmentslottype : EquipmentSlotType.values()) {
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
                    map = Maps.newEnumMap(EquipmentSlotType.class);
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

    private void handleEquipmentChanges(Map<EquipmentSlotType, ItemStack> p_241344_1_) {
        List<Pair<EquipmentSlotType, ItemStack>> list = Lists.newArrayListWithCapacity(p_241344_1_.size());
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
        ((ServerWorld)this.level).getChunkSource().broadcast(this, new SEntityEquipmentPacket(this.getId(), list));
    }

    private ItemStack getLastHandItem(EquipmentSlotType p_241347_1_) {
        return this.lastHandItemStacks.get(p_241347_1_.getIndex());
    }

    private void setLastHandItem(EquipmentSlotType p_241345_1_, ItemStack p_241345_2_) {
        this.lastHandItemStacks.set(p_241345_1_.getIndex(), p_241345_2_);
    }

    private ItemStack getLastArmorItem(EquipmentSlotType p_241346_1_) {
        return this.lastArmorItemStacks.get(p_241346_1_.getIndex());
    }

    private void setLastArmorItem(EquipmentSlotType p_241343_1_, ItemStack p_241343_2_) {
        this.lastArmorItemStacks.set(p_241343_1_.getIndex(), p_241343_2_);
    }
}