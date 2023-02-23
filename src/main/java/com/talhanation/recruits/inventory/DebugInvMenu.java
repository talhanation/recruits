package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DebugInvMenu extends ContainerBase {

    private final Container container;
    private final AbstractRecruitEntity recruit;

    public DebugInvMenu(int id, AbstractRecruitEntity recruit, Inventory playerInventory) {
        super(ModScreens.DEBUG_CONTAINER_TYPE.get(), id, playerInventory, recruit.getInventory());
        this.recruit = recruit;
        this.container = recruit.getInventory();

        addPlayerInventorySlots();
        addWorkerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public void addWorkerInventorySlots() {
        for (int k = 0; k < 2; ++k) {
            for (int l = 0; l < 9; ++l) {
                if (k == 1 && l == 6)break;
                int slot = l + k * 9;
                this.addSlot(new Slot(container, slot, 8 + l * 18,  3 + 18 * 5 + k * 18){
                    @Override
                    public void set(ItemStack stack) {
                        super.set(stack);
                        if(slot <= 5) {
                            EquipmentSlot equipmentSlot = recruit.getEquipmentSlotIndex(slot);
                            recruit.setItemSlot(equipmentSlot, stack);
                        }
                    }
                });
            }
        }
    }

    public AbstractRecruitEntity getRecruit() {
        return recruit;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return this.container.stillValid(playerIn) && this.recruit.isAlive();
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }

    public void broadcastChanges() {
        super.broadcastChanges();
    }
}
