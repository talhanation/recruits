package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.util.Hand;

public class RecruitEatGoal extends Goal {

    AbstractRecruitEntity recruit;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return  recruit.getHealth() < recruit.getMaxHealth();
    }

    @Override
    public void start() {
        recruit.setItemInHand(Hand.OFF_HAND, Items.COOKED_CHICKEN.getDefaultInstance());
        recruit.startUsingItem(Hand.OFF_HAND);
        //recruit.heal(100);
    }

    @Override
    public void stop() {
        recruit.stopUsingItem();
    }

}
