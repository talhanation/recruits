package com.talhanation.recruits.inventory;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;

public class MessengerContainer extends ContainerBase {

    private final Player playerEntity;
    private final MessengerEntity recruit;

    public MessengerContainer(int id, Player playerEntity, MessengerEntity messenger) {
        super(ModScreens.MESSENGER.get(), id, playerEntity.getInventory(), messenger.getInventory());
        this.playerEntity = playerEntity;
        this.recruit = messenger;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public MessengerEntity getRecruit() {
        return recruit;
    }
}
