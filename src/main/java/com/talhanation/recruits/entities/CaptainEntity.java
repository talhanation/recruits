package com.talhanation.recruits.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public class CaptainEntity extends AbstractLeaderEntity {

    public CaptainEntity(EntityType<? extends AbstractLeaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return null;
    }

    @Override
    public AbstractRecruitEntity get() {
        return null;
    }

    @Override
    public void openSpecialGUI(Player player) {

    }
}










