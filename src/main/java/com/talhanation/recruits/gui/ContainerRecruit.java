package com.talhanation.recruits.gui;

import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ContainerType;

public class ContainerRecruit extends ContainerBase {

    public ContainerRecruit(ContainerType containerType, int id, IInventory playerInventory, IInventory inventory) {
        super(containerType, id, playerInventory, inventory);
    }
}