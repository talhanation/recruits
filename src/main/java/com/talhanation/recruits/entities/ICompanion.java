package com.talhanation.recruits.entities;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface ICompanion {

/*
    enum CompanionProfession {
        UNPROMOTED(0),
        MESSENGER(1),
        PATROL_LEADER(2),
        CAPTIAN(3),
        SCOUT(4),
        GOVERNOR(5),
        ASSASSIN(6),
        SPY(7),
        SIEGE_ENGINEER(8),
        ROGUE(9)
    }
 */

    static void assignToLeaderCompanion(AbstractLeaderEntity leader, AbstractRecruitEntity recruit) {
        recruit.setListen(false);
        recruit.clearHoldPos();
        recruit.setProtectUUID(Optional.of(leader.getUUID()));
        recruit.setFollowState(5);

        if(leader.getUpkeepUUID() != null) recruit.setUpkeepUUID(Optional.of(leader.getUpkeepUUID()));
        if(leader.getUpkeepPos() != null) recruit.setUpkeepPos(leader.getUpkeepPos());

        recruit.setTarget(null);
        recruit.setState(leader.getState());
    }

    AbstractRecruitEntity get();
    void openSpecialGUI(Player player);
    String getOwnerName();
    void setOwnerName(String name);
    boolean isAtMission();
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
        this.get().setBiome((byte) recruit.getBiome());

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
        float newMorale = recruit.getMorale() + 20F;
        if(newMorale > 100F) newMorale = 100F;
        this.get().setMoral(newMorale);

        this.get().heal(100);
    }
}
