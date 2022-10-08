package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;
import java.util.Optional;

public class RecruitEatGoal extends Goal {

    public AbstractRecruitEntity recruit;
    public ItemStack foodStack;
    public ItemStack beforeFoodItem;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return hasFoodInInv() && recruit.needsToEat() && !recruit.getIsEating();
    }

    @Override
    public boolean canContinueToUse() {
        return recruit.getIsEating() && hasFoodInInv() && recruit.needsToEat();
    }

    public boolean isInterruptable() {
        return false;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        beforeFoodItem = recruit.getItemInHand(InteractionHand.OFF_HAND);
        recruit.setIsEating(true);
        this.foodStack = getFoodInInv();


        recruit.heal(Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getSaturationModifier() * 1);
        if (!recruit.isSaturated())
            recruit.setHunger(recruit.getHunger() + Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, recruit)).getSaturationModifier() * 10);


        //Main.LOGGER.debug("Start: beforeFoodItem: " + beforeFoodItem);
        //Main.LOGGER.debug("Start: foodStack: " + foodStack);

        recruit.setItemInHand(InteractionHand.OFF_HAND, foodStack);
        recruit.startUsingItem(InteractionHand.OFF_HAND);
    }


    private boolean hasFoodInInv(){
        return recruit.getInventory().items
                .stream()
                .anyMatch(ItemStack::isEdible);
    }

    private ItemStack getFoodInInv(){
        Optional<ItemStack> itemStack = recruit.getInventory().items
                .stream()
                .filter(ItemStack::isEdible)
                .findAny();

        assert itemStack.isPresent();
        return itemStack.get();
    }

    @Override
    public void tick() {
        super.tick();

        if(!recruit.isUsingItem() && recruit.getIsEating() && beforeFoodItem != null) stop();
    }

    @Override
    public void stop() {
        recruit.setIsEating(false);
        recruit.stopUsingItem();

        resetItemInHand();
        recruit.eatCoolDown = 100;
    }

    public void resetItemInHand() {
        recruit.setItemInHand(InteractionHand.OFF_HAND, this.beforeFoodItem);
        recruit.inventory.setItem(10, this.beforeFoodItem);
    }
}