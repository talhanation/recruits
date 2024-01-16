package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

public class PromoteContainer extends ContainerBase {

    private final Player playerEntity;
    private final AbstractRecruitEntity recruit;

    public PromoteContainer(int id, Player playerEntity, AbstractRecruitEntity recruit) {
        super(ModScreens.PROMOTE.get(), id, playerEntity.getInventory(), new SimpleContainer(0));
        this.playerEntity = playerEntity;
        this.recruit = recruit;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public AbstractRecruitEntity getRecruit() {
        return recruit;
    }
}
