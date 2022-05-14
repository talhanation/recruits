package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;

public class CommandContainer extends ContainerBase {


    private Player playerEntity;

    public CommandContainer(int id, Player playerEntity) {
        super(Main.COMMAND_CONTAINER_TYPE, id, null, null);{
        //super(Main.COMMAND_CONTAINER_TYPE, id, null, new Inventory(0)); // 1.16 code
            this.playerEntity = playerEntity;
        }
    }
    public Player getPlayerEntity() {
        return playerEntity;
    }
}