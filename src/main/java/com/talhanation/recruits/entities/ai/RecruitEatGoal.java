package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;

public class RecruitEatGoal extends Goal {

    AbstractRecruitEntity recruit;
    ItemStack foodItem = null;
    ItemStack beforFoodItem = null;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        float currentHealth =  recruit.getHealth();
        float maxHealth = recruit.getMaxHealth();

        if (recruit.getTarget() != null){
            return (currentHealth <  maxHealth - maxHealth / 1.75);
        }
        else {
            return (currentHealth <  maxHealth - maxHealth / 5.0D);
        }
    }


    @Override
    public void start() {
        if (hasFoodInInv() && foodItem != null) {
            ItemStack itemStack = recruit.getItemInHand(Hand.OFF_HAND);
            this.beforFoodItem = itemStack.copy();
            recruit.setItemInHand(Hand.OFF_HAND, foodItem.getItem().getDefaultInstance());

            for (int i = 0; i < 64; i++) recruit.startUsingItem(Hand.OFF_HAND);

            if (recruit.isUsingItem()) {
                recruit.heal(foodItem.getItem().getFoodProperties().getSaturationModifier() * 10);
                recruit.level.playSound(null, recruit.getX(), recruit.getY(), recruit.getZ(), foodItem.getEatingSound(), SoundCategory.MUSIC, 15.0F, 0.8F + 0.4F * recruit.getRandom().nextFloat());
            }
        }
    }

    @Override
    public void stop() {
        recruit.stopUsingItem();
        if (foodItem != null) {
            foodItem.shrink(1);
            resetItemInHand();
        }

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


    private void resetItemInHand(){
        recruit.setItemInHand(Hand.OFF_HAND, beforFoodItem.getItem().getDefaultInstance());
    }

}
