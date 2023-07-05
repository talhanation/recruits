package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class AssassinLeaderMenu extends ContainerBase {

    private final Container recruitInventory;
    private final AssassinLeaderEntity assassinLeaderEntity;

    public AssassinLeaderMenu(int id, AssassinLeaderEntity assassinLeaderEntity, Inventory playerInventory) {
        super(ModScreens.ASSASSIN_CONTAINER_TYPE.get(), id, playerInventory, assassinLeaderEntity.getInventory());
        this.assassinLeaderEntity = assassinLeaderEntity;
        this.recruitInventory = assassinLeaderEntity.getInventory();

        addPlayerInventorySlots();
    }

    public AssassinLeaderEntity getEntity() {
        return assassinLeaderEntity;
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return this.recruitInventory.stillValid(playerIn) && this.assassinLeaderEntity.isAlive() && this.assassinLeaderEntity.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }
}