package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

public class RecruitEatGoal extends Goal {

    public AbstractRecruitEntity recruit;
    public ItemStack foodStack;
    public ItemStack beforeItem;
    public int slotID;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return hasFoodInInv() && recruit.needsToEat() && !recruit.getIsEating() && !recruit.isUsingItem();
    }

    @Override
    public boolean canContinueToUse() {
        return recruit.isUsingItem();
    }

    public boolean isInterruptable() {
        return false;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        slotID = 0;
        beforeItem = recruit.getOffhandItem().copy();
        recruit.setIsEating(true);
        this.foodStack = getAndRemoveFoodInInv().copy();
        /*
        Main.LOGGER.debug("Start--------------: ");
        Main.LOGGER.debug("beforeFoodItem: " + beforeFoodItem.copy());
        Main.LOGGER.debug("isEating: " + recruit.getIsEating());
        Main.LOGGER.debug("foodStack: " + foodStack.copy());
        Main.LOGGER.debug("Start--------------:");
        */

        recruit.heal(Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getSaturationModifier() * 1);
        if (!recruit.isSaturated())
            recruit.setHunger(recruit.getHunger() + Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getSaturationModifier() * 10);


        recruit.setItemInHand(InteractionHand.OFF_HAND, foodStack);
        recruit.startUsingItem(InteractionHand.OFF_HAND);
    }

    @Override
    public void stop() {
        recruit.setIsEating(false);
        recruit.stopUsingItem();

        if(recruit.getMoral() < 100){
            recruit.setMoral(recruit.getMoral() + 1.5F);
        }

        resetItemInHand();
        /*
        Main.LOGGER.debug("Stop--------------: ");
        Main.LOGGER.debug("beforeFoodItem: " + beforeFoodItem);
        Main.LOGGER.debug("isEating: " + recruit.getIsEating());
        Main.LOGGER.debug("foodStack: " + foodStack.copy());
        Main.LOGGER.debug("Stop--------------:");

         */
    }

    public void resetItemInHand() {
        recruit.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        recruit.inventory.setItem(4, ItemStack.EMPTY);

        recruit.setItemInHand(InteractionHand.OFF_HAND, this.beforeItem.copy());
        recruit.inventory.setItem(slotID, foodStack.copy());

    }

    private boolean hasFoodInInv(){
        return recruit.getInventory().items
                .stream()
                .anyMatch(ItemStack::isEdible);
    }

    private ItemStack getAndRemoveFoodInInv(){
        ItemStack itemStack = null;
        for(int i = 0; i < recruit.getInventorySize(); i++){
            ItemStack stackInSlot = recruit.inventory.getItem(i).copy();
            if(stackInSlot.isEdible()){
                itemStack = stackInSlot.copy();
                this.slotID = i;
                recruit.inventory.removeItemNoUpdate(i); //removing item in slot
                break;
            }
        }
        return itemStack;
    }
}
