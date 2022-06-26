package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class RecruitEatGoal extends Goal {

    AbstractRecruitEntity recruit;
    ItemStack foodStack;

    public RecruitEatGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return hasFoodInInv() && recruit.needsToEat() && !recruit.isUsingItem();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        recruit.beforeFoodItem = recruit.getItemInHand(InteractionHand.OFF_HAND);
        foodStack = getFoodInInv();
        recruit.setIsEating(true);
        recruit.setItemInHand(InteractionHand.OFF_HAND, foodStack);
        recruit.getSlot(10).set(foodStack);
        recruit.startUsingItem(InteractionHand.OFF_HAND);

        recruit.heal(foodStack.getItem().getFoodProperties(foodStack, recruit).getSaturationModifier() * 10);
        if(!recruit.isSaturated())recruit.setHunger(recruit.getHunger() + foodStack.getItem().getFoodProperties(foodStack, recruit).getSaturationModifier() * 100);
        if(foodStack.getCount() == 1)foodStack.shrink(1);//fix infinite food?
        recruit.eatCoolDown = 100;
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
    }
}