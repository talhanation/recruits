package com.talhanation.recruits.gui;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ContainerType;

public class RecruitContainer extends ContainerBase {

    protected AbstractRecruitEntity recruit;

    public RecruitContainer(ContainerType containerType, int id, IInventory playerInventory, IInventory inventory) {
        super(containerType, id, playerInventory, inventory);
    }

    /*
    public RecruitContainer(int id, AbstractRecruitEntity recruit, Inventory playerInv) {
        super(Main.PLANE_CONTAINER_TYPE, id, playerInv, recruit.getInventory());
        this.recruit = recruit;

        int numRows = plane.getInventory().getContainerSize() / 9;

        for (int j = 0; j < numRows; j++) {
            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(plane.getInventory(), k + j * 9, 8 + k * 18, 72 + j * 18));
            }
        }

        addPlayerInventorySlots();
    }
*/
    public AbstractRecruitEntity getRecruit() {
        return recruit;
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

}