package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public class DebugInvContainer extends ContainerBase {

    private final Container container;
    private final AbstractRecruitEntity recruit;

    public DebugInvContainer(int id, AbstractRecruitEntity recruit, Inventory playerInventory) {
        super(Main.DEBUG_CONTAINER_TYPE, id, playerInventory, recruit.getInventory());
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
                this.addSlot(new Slot(container, l + k * 9, 8 + l * 18,  3 + 18 * 5 + k * 18));
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
