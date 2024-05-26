package com.talhanation.recruits.inventory;

import com.talhanation.recruits.entities.AbstractInventoryEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.Objects;

import static net.minecraft.world.entity.LivingEntity.getEquipmentSlotForItem;

public class RecruitSimpleContainer extends SimpleContainer {
    private final AbstractInventoryEntity recruit;
    private final int size;
    public RecruitSimpleContainer(int inventorySize, AbstractInventoryEntity abstractInventoryEntity) {
        super(inventorySize);
        this.size = inventorySize;
        this.recruit = abstractInventoryEntity;
    }

    public ItemStack addItem(ItemStack itemStack) {
        ItemStack itemstack = itemStack.copy();
        this.moveItemToOccupiedSlotsWithSameType(itemstack);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.moveItemToEmptySlots(itemstack);
            return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
        }
    }

    private void moveItemToEmptySlots(ItemStack itemStack) {
        for(int i = 0; i < this.size; ++i) {
            if(i == 4 || i == 5) continue;
            ItemStack itemstack = this.getItem(i);
            if (itemstack.isEmpty() && canPlaceItem(i, itemStack)) {
                this.setItem(i, itemStack.copy());
                if(i < 6) {
                    recruit.setItemSlot(Objects.requireNonNull(recruit.getEquipmentSlotIndex(i)), itemStack);
                }
                itemStack.setCount(0);
                return;
            }
        }
    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack itemStack) {
        for(int i = 0; i < this.size; ++i) {
            ItemStack itemstack = this.getItem(i);
            if (ItemStack.isSameItemSameTags(itemstack, itemStack)) {
                this.moveItemsBetweenStacks(itemStack, itemstack);
                if (itemStack.isEmpty()) {
                    return;
                }
            }
        }
    }

    private void moveItemsBetweenStacks(ItemStack itemStack1, ItemStack itemStack2) {
        int i = Math.min(this.getMaxStackSize(), itemStack2.getMaxStackSize());
        int j = Math.min(itemStack1.getCount(), i - itemStack2.getCount());
        if (j > 0) {
            itemStack2.grow(j);
            itemStack1.shrink(j);
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        if(slot == 0|| slot == 1 || slot == 2 || slot == 3) {
            EquipmentSlot equipmentslottype = RecruitInventoryMenu.SLOT_IDS[slot];
            return itemStack.canEquip(equipmentslottype, recruit); //|| (itemStack.getItem() instanceof BannerItem && equipmentslottype.equals(EquipmentSlot.HEAD))
        }
        else if(slot == 4)//offhand
            return (itemStack.getItem() instanceof ShieldItem);

        else if (slot == 5)//mainhand
            return recruit.canHoldItem(itemStack);

        else
            return true;
    }
}
