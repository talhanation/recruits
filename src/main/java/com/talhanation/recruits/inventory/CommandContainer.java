package com.talhanation.recruits.inventory;

import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class CommandContainer extends ContainerBase {


    public CommandContainer(int id, PlayerEntity playerEntity, null) {
        super(Main.COMMAND_CONTAINER, id, null, null);
        this.recruit = recruit;
        this.recruitInventory = recruit.getInventory();

        addRecruitInventorySlots();
        addRecruitHandSlots();
        addRecruitEquipmentSlots();
        addPlayerInventorySlots();
    }

    public AbstractRecruitEntity getRecruit() {
        return recruit;
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public void addRecruitInventorySlots() {
        for (int k = 0; k < 3; ++k) {
            for (int l = 0; l < 3; ++l) {
                this.addSlot(new Slot(recruitInventory, 0 + l + k * recruit.getInventoryColumns(), 2 * 18 + 82 + l * 18,  18 + k * 18));
            }
        }
    }

    public void addRecruitEquipmentSlots() {
        for (int k = 0; k < 4; ++k) {
            final EquipmentSlotType equipmentslottype = SLOT_IDS[k];
            this.addSlot(new Slot(recruit.inventory, 11 + k, 8, 18 + k * 18) {
                public int getMaxStackSize() {
                    return 1;
                }

                public boolean mayPlace(ItemStack itemStack) {
                    return itemStack.canEquip(equipmentslottype, recruit);
                }

                @Override
                public void set(ItemStack stack){
                    super.set(stack);
                    recruit.setItemSlot(equipmentslottype, stack);
                }

                @OnlyIn(Dist.CLIENT)
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(PlayerContainer.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentslottype.getIndex()]);
                }
            });
        }

    }

    public void addRecruitHandSlots() {
        this.addSlot(new Slot(recruit.inventory, 9,26,90) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return true;
            }

            @Override
            public void set(ItemStack stack){
                super.set(stack);
                recruit.setItemSlot(EquipmentSlotType.MAINHAND, stack);
            }

        });

        this.addSlot(new Slot(recruit.inventory, 10,44,90) {
        @Override
        public boolean mayPlace(ItemStack stack){
            return stack.isShield(null);
        }

        @Override
        public void set(ItemStack stack){
            super.set(stack);
            recruit.setItemSlot(EquipmentSlotType.OFFHAND, stack);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon () {
            return Pair.of(PlayerContainer.BLOCK_ATLAS, PlayerContainer.EMPTY_ARMOR_SLOT_SHIELD);
        }
        });
    }


    @Override
    public boolean stillValid(PlayerEntity playerIn) {
        return this.recruitInventory.stillValid(playerIn) && this.recruit.isAlive() && this.recruit.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(PlayerEntity playerIn) {
        super.removed(playerIn);
    }
}