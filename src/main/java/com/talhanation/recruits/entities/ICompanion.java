package com.talhanation.recruits.entities;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface ICompanion {

    AbstractRecruitEntity get();
    void openSpecialGUI(Player player);
    String getOwnerName();
    void setOwnerName(String name);
    Byte getTaskState();
    void setTaskState(Byte x);
    default void applyRecruitValues(AbstractRecruitEntity recruit){
        //ATTRIBUTES
        this.get().getAttributes().assignValues(recruit.getAttributes());

        this.get().setHunger(recruit.getHunger());
        this.get().setVariant(recruit.getVariant());

        this.get().setOwnerUUID(Optional.of(recruit.getOwnerUUID()));
        if(recruit.getUpkeepPos() != null) get().setUpkeepPos(recruit.getUpkeepPos());
        if(recruit.getUpkeepUUID() != null) get().setUpkeepUUID(Optional.of(recruit.getUpkeepUUID()));
        this.get().updateTeam();
        this.get().setIsOwned(true);
        if(recruit.getHoldPos() != null) get().setHoldPos(recruit.getHoldPos());
        if(recruit.getMovePos() != null) get().setMovePos(recruit.getMovePos());
        this.get().setGroup(recruit.getGroup());
        this.get().setKills(recruit.getKills());
        this.get().setXp(recruit.getXp());
        this.get().setXpLevel(recruit.getXpLevel());

        this.get().setState(recruit.getState());
        this.get().setFollowState(recruit.getFollowState());
        this.get().setListen(recruit.getListen());

        //INVENTORY
        for(int i = 0; i < recruit.getInventory().getContainerSize(); i++){
            ItemStack itemStack = recruit.getInventory().getItem(i);

            if(i > 5){
                this.get().getInventory().setItem(i, itemStack);
            }
            else
                this.get().setItemSlot(this.get().getEquipmentSlotIndex(i), itemStack);
        }
        
        //MORALE
        float newMorale = recruit.getMoral() + 20F;
        if(newMorale > 100F) newMorale = 100F;
        this.get().setMoral(newMorale);

        this.get().heal(100);
    }
}
