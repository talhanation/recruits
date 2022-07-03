package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class RecruitHireContainer extends ContainerBase {


    private final Player playerEntity;
    private final AbstractRecruitEntity recruit;

    public RecruitHireContainer(int id, Player playerEntity, AbstractRecruitEntity recruit, Inventory playerInventory) {
        super(Main.HIRE_CONTAINER_TYPE, id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
        this.recruit = recruit;
        this.playerInventory = playerInventory;

        addPlayerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public AbstractRecruitEntity getRecruitEntity() {
        return recruit;
    }
}