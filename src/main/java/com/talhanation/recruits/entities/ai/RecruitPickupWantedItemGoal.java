package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;


public class RecruitPickupWantedItemGoal extends Goal{

    AbstractRecruitEntity recruit;

    public RecruitPickupWantedItemGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return (recruit.getTarget() == null || !recruit.isBlocking());
    }

    @Override
    public void tick() {
        List<ItemEntity> list = recruit.level.getEntitiesOfClass(ItemEntity.class, recruit.getBoundingBox().inflate(6.0D, 3.0D, 6.0D), recruit.getAllowedItems());
        if (!list.isEmpty()) {
             if (recruit.getHunger() < 30) recruit.getNavigation().moveTo(list.get(0), 1.15F);
        }
    }
}