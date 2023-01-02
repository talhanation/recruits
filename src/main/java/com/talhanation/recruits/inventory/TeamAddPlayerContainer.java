package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

public class TeamAddPlayerContainer extends ContainerBase {

    private Player playerEntity;

    public TeamAddPlayerContainer(int id, Player playerEntity) {
        super(Main.TEAM_ADD_PLAYER_TYPE, id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }
}
