package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;

public class CommandContainer extends ContainerBase {


    private Player playerEntity;

    public CommandContainer(int id, Player playerEntity) {
        super(Main.COMMAND_CONTAINER_TYPE, id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }
}