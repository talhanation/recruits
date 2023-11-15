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
        return hasFoodInInv() && recruit.needsToEat() && !recruit.isUsingItem();
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
        //get OffhandItem
        ItemStack beforeItem = recruit.getOffhandItem().copy();
        //get FoodItem and slot in inv and remove item
        this.foodStack = getAndRemoveFoodInInv().copy();
        //save slot to compound
        this.recruit.setBeforeItemSlot(slotID);
        //set beforeItem In Inventory slot
        this.recruit.getInventory().setItem(this.recruit.getBeforeItemSlot(), beforeItem);
        //set foodstack in inventory
        this.recruit.setItemInHand(InteractionHand.OFF_HAND, foodStack);
        //this.recruit.getInventory().setItem(4, foodStack);//inventory slot

        this.recruit.startUsingItem(InteractionHand.OFF_HAND);
        /*
        Main.LOGGER.debug("Start--------------: ");
        Main.LOGGER.debug("beforeFoodItem: " + beforeFoodItem.copy());
        Main.LOGGER.debug("isEating: " + recruit.getIsEating());
        Main.LOGGER.debug("foodStack: " + foodStack.copy());
        Main.LOGGER.debug("Start--------------:");
        */

        recruit.heal(Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getSaturationModifier() * 1);
        if (!recruit.isSaturated()){
            float saturation = Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getSaturationModifier();
            float nutrition = Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getNutrition() * 5;

            float currentHunger = recruit.getHunger();
            float newHunger = currentHunger + saturation + nutrition;

            if(newHunger > 100) newHunger = 100;

            recruit.setHunger(newHunger);
        }
    }

    @Override
    public void stop() {
        recruit.stopUsingItem();

        if(recruit.getMoral() < 100){
            recruit.setMoral(recruit.getMoral() + 2.5F);
        }

        recruit.resetItemInHand();
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
