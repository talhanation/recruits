package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

public class RecruitEatGoal extends Goal {

    public AbstractRecruitEntity recruit;
    public ItemStack foodStack;
    public int slotID;
    private long lastCanUseCheckMorale;
    private long lastCanUseCheckHunger;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        long i = this.recruit.getCommandSenderWorld().getGameTime();
        if(i - this.lastCanUseCheckMorale >= 1200L){
            this.lastCanUseCheckMorale = i;
            this.recruit.updateMorale();
        }
        if (i - this.lastCanUseCheckHunger >= 20L) {
            this.lastCanUseCheckHunger = i;
            this.recruit.updateHunger();
            return hasFoodInInv() && recruit.needsToEat() && !recruit.isUsingItem();
        }
        
        return false;
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

        FoodProperties foodProperties = Objects.requireNonNull(foodStack.get(DataComponents.FOOD));
        recruit.heal(foodProperties.saturation() * 1);
        if (!recruit.isSaturated()){
            float saturation = foodProperties.saturation();
            float nutrition = foodProperties.nutrition() * 5;

            float currentHunger = recruit.getHunger();
            float newHunger = currentHunger + saturation + nutrition;

            if(newHunger > 100) newHunger = 100;

            recruit.setHunger(newHunger);
        }
    }

    @Override
    public void stop() {
        recruit.stopUsingItem();

        if(recruit.getMorale() < 100){
            recruit.setMoral(recruit.getMorale() + 2.5F);
        }

        recruit.resetItemInHand();
    }

    private boolean hasFoodInInv(){
        return recruit.getInventory().items()
                .stream()
                .anyMatch(item -> recruit.canEatItemStack(item));
    }

    private ItemStack getAndRemoveFoodInInv(){
        ItemStack itemStack = null;
        for(int i = 0; i < recruit.getInventorySize(); i++){
            ItemStack stackInSlot = recruit.inventory.getItem(i).copy();
            if(recruit.canEatItemStack(stackInSlot)){
                itemStack = stackInSlot.copy();
                this.slotID = i;
                recruit.inventory.removeItemNoUpdate(i); //removing item in slot
                break;
            }
        }
        return itemStack;
    }
}
