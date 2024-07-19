package com.talhanation.recruits.inventory;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;

public class MessengerAnswerContainer extends ContainerBase {

    private final Player playerEntity;
    private final MessengerEntity recruit;

    public MessengerAnswerContainer(int id, Player playerEntity, MessengerEntity messenger) {
        super(ModScreens.MESSENGER_ANSWER.get(), id, playerEntity.getInventory(), messenger.getInventory());
        this.playerEntity = playerEntity;
        this.recruit = messenger;
        this.recruit.targetPlayerOpened = true;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public MessengerEntity getRecruit() {
        return recruit;
    }
}
