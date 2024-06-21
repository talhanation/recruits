package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.entities.PatrolLeaderEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PatrolLeaderContainer extends ContainerBase {
    private final Player playerEntity;
    private final AbstractLeaderEntity recruit;

    public PatrolLeaderContainer(int id, Player playerEntity, AbstractLeaderEntity leader) {
        super(ModScreens.PATROL_LEADER.get(), id, playerEntity.getInventory(), new SimpleContainer(0));
        this.playerEntity = playerEntity;
        this.recruit = leader;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public AbstractLeaderEntity getRecruit() {
        return recruit;
    }
}
