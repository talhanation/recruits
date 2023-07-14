package com.talhanation.recruits.inventory;

import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class DisbandContainer extends ContainerBase {
    private Player playerEntity;
    private UUID recruit;

    public DisbandContainer(int id, Player playerEntity, UUID recruit) {
        super(ModScreens.DISBAND.get(), id, playerEntity.getInventory(), new SimpleContainer(0));
        this.playerEntity = playerEntity;
        this.recruit = recruit;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public UUID getRecruit() {
        return recruit;
    }
}