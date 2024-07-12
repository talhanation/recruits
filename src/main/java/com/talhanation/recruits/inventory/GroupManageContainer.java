package com.talhanation.recruits.inventory;

import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

public class GroupManageContainer extends ContainerBase {

    private Player playerEntity;

    public GroupManageContainer(int id, Player playerEntity) {
        super(ModScreens.GROUP_MANAGE_TYPE.get(), id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }
}