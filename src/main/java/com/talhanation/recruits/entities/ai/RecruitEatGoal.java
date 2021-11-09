package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class RecruitEatGoal extends Goal {

    AbstractRecruitEntity recruit;
    ItemStack foodItem;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        if (recruit.isUsingItem()) return false;

        float currentHealth =  recruit.getHealth();
        float maxHealth = recruit.getMaxHealth();

        if (recruit.getTarget() != null){
            return (currentHealth <  maxHealth - maxHealth / 1.75);
        }
        else
            return (currentHealth <  maxHealth - maxHealth / 5.0D);

    }

    @Override
    public void start() {
        if (hasFoodInInv()) {
            recruit.beforeFoodItem = recruit.getItemInHand(Hand.OFF_HAND);

            recruit.setIsEating(true);
            recruit.setItemInHand(Hand.OFF_HAND, foodItem.getItem().getDefaultInstance());

            recruit.startUsingItem(Hand.OFF_HAND);

            recruit.heal(foodItem.getItem().getFoodProperties().getSaturationModifier() * 10);

            recruit.eat(recruit.level, foodItem);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean hasFoodInInv(){
        Inventory inventory = recruit.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.isEdible()){
                foodItem = itemStack;

                return true;
            }
        }
        return false;
    }
}
