package com.talhanation.recruits.inventory;

import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AssassinLeaderContainer extends ContainerBase {


    private final IInventory recruitInventory;
    private final AssassinLeaderEntity assassinLeaderEntity;

    public AssassinLeaderContainer(int id, AssassinLeaderEntity assassinLeaderEntity, PlayerInventory playerInventory) {
        super(Main.ASSASSIN_CONTAINER_TYPE, id, playerInventory, assassinLeaderEntity.getInventory());
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
    public boolean stillValid(PlayerEntity playerIn) {
        return this.recruitInventory.stillValid(playerIn) && this.assassinLeaderEntity.isAlive() && this.assassinLeaderEntity.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(PlayerEntity playerIn) {
        super.removed(playerIn);
    }
}