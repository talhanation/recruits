package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractInventoryEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;

import java.util.List;
import java.util.function.Predicate;


public class RecruitPickupWantedItemGoal extends Goal{

    AbstractInventoryEntity recruit;
    Predicate<ItemEntity> allowedItems;

    public RecruitPickupWantedItemGoal(AbstractInventoryEntity recruit, Predicate<ItemEntity> allowedItems) {
        this.recruit = recruit;
        this.allowedItems = allowedItems;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        List<ItemEntity> list = recruit.level.getEntitiesOfClass(ItemEntity.class, recruit.getBoundingBox().inflate(16.0D, 8.0D, 16.0D), allowedItems);
        if (!list.isEmpty()) {
            recruit.getNavigation().moveTo(list.get(0), 1.15F);
        }
    }
}