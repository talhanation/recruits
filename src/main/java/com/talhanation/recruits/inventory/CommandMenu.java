package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;

public class CommandMenu extends ContainerBase {


    private Player playerEntity;

    public CommandMenu(int id, Player playerEntity) {
        super(ModScreens.COMMAND_CONTAINER_TYPE.get(), id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }
}