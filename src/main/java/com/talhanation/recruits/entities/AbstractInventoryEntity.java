package com.talhanation.recruits.entities;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class AbstractInventoryEntity extends PathfinderMob {


    //iv slots
    //9,10 = hand
    //11,12,13,14 = armor
    //0-8 = inv

    public SimpleContainer inventory;
    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;
    private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);

    public AbstractInventoryEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
        this.createInventory();
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
    }

    ////////////////////////////////////DATA////////////////////////////////////

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
        ListTag listnbt = nbt.getList("Items", 10);//muss 10 sein amk sonst nix save
        this.createInventory();

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundTag compoundnbt = listnbt.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;
            if (j < this.inventory.getContainerSize()) {
                this.inventory.setItem(j, ItemStack.of(compoundnbt));
            }
        }

        ListTag armorItems = nbt.getList("ArmorItems", 10);
        for (int i = 0; i < this.armorItems.size(); ++i) {
            int index = this.getInventorySlotIndex(Mob.getEquipmentSlotForItem(ItemStack.of(armorItems.getCompound(i))));
            this.inventory.setItem(index, ItemStack.of(armorItems.getCompound(i)));
        }

        ListTag handItems = nbt.getList("HandItems", 10);
        for (int i = 0; i < this.handItems.size(); ++i) {
            int index = i == 0 ? 4 : 5;
            this.inventory.setItem(index, ItemStack.of(handItems.getCompound(i)));
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

    public int getInventorySlotIndex(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:
                return 0;
            case CHEST:
                return 1;
            case LEGS:
                return 2;
            case FEET:
                return 3;
            default:
                break;
        }
        return 0;
    }

    ////////////////////////////////////SET////////////////////////////////////

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        super.setItemSlot(slotIn, stack);
        switch (slotIn) {
            case HEAD ->{
                if (this.inventory.getItem(0).isEmpty())
                    this.inventory.setItem(0, this.armorItems.get(slotIn.getIndex()));
            }
            case CHEST-> {
                if (this.inventory.getItem(1).isEmpty())
                    this.inventory.setItem(1, this.armorItems.get(slotIn.getIndex()));
            }
            case LEGS-> {
                if (this.inventory.getItem(2).isEmpty())
                    this.inventory.setItem(2, this.armorItems.get(slotIn.getIndex()));
            }
            case FEET-> {
                if (this.inventory.getItem(3).isEmpty())
                    this.inventory.setItem(3, this.armorItems.get(slotIn.getIndex()));
            }
            case MAINHAND-> {
                if (this.inventory.getItem(4).isEmpty())
                    this.inventory.setItem(4, this.handItems.get(slotIn.getIndex()));
            }
            case OFFHAND-> {
                if (this.inventory.getItem(5).isEmpty())
                    this.inventory.setItem(5, this.handItems.get(slotIn.getIndex()));
            }
        }
    }
    public @NotNull SlotAccess getSlot(int slot) {
        return slot == 499 ? new SlotAccess() {
            public ItemStack get() {
                return new ItemStack(Items.CHEST);
            }

            public boolean set(ItemStack stack) {
                if (stack.isEmpty()) {

                    AbstractInventoryEntity.this.createInventory();

                    return true;
                } else {
                    return false;
                }
            }
        } : super.getSlot(slot);
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
                itemEntity.remove(RemovalReason.KILLED);
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
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemHandler != null) {
            LazyOptional<?> oldHandler = itemHandler;
            itemHandler = null;
            oldHandler.invalidate();
        }
    }
}