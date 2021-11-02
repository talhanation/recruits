package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;

public class RecruitEatGoal extends Goal {

    AbstractRecruitEntity recruit;
    ItemStack foodItem = null;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        float currentHealth =  recruit.getHealth();
        float maxHealth = recruit.getMaxHealth();


        return  currentHealth <  maxHealth - maxHealth / 1.75;
    }

    @Override
    public void start() {
        if (hasFoodInInv() && foodItem != null) {
            recruit.setItemInHand(Hand.OFF_HAND, foodItem.getItem().getDefaultInstance());
            recruit.startUsingItem(Hand.OFF_HAND);
            recruit.heal(foodItem.getItem().getFoodProperties().getSaturationModifier() * 10);
            recruit.level.playSound(null, recruit.getX(), recruit.getY() , recruit.getZ(), foodItem.getEatingSound(), SoundCategory.NEUTRAL, 15.0F, 0.8F + 0.4F * recruit.getRandom().nextFloat());
        }
    }

    @Override
    public void stop() {
        recruit.stopUsingItem();
        foodItem.shrink(1);
    }


    private boolean hasFoodInInv(){
        Inventory inventory = recruit.getInventory();
        boolean flag = false;

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
