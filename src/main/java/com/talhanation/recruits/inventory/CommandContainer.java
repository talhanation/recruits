package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;

public class CommandContainer extends ContainerBase {


    private PlayerEntity playerEntity;

    public CommandContainer(int id, PlayerEntity playerEntity) {
        super(Main.COMMAND_CONTAINER_TYPE, id, null, new Inventory(0));
        this.playerEntity = playerEntity;
    }

    public PlayerEntity getPlayerEntity() {
        return playerEntity;
    }
}