package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.InteractionHand;

import javax.annotation.Nullable;

public class RecruitQuaffGoal extends Goal {

    public AbstractRecruitEntity recruit;
    public ItemStack potionItem;
    public ItemStack beforeItem;

    public RecruitQuaffGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return hasPotionInInv() && recruit.needsToPotion() && recruit.getTarget() != null && !recruit.getIsEating();
    }

    @Override
    public boolean canContinueToUse() {
        return recruit.getIsEating() && hasPotionInInv() && recruit.needsToPotion() && recruit.getTarget() != null;
    }

    public boolean isInterruptable() {
        return false;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.beforeItem = recruit.getItemInHand(InteractionHand.OFF_HAND);
        this.recruit.setIsEating(true);
        this.potionItem = getPotionInInv();

        Main.LOGGER.debug("Start: beforeItem: " + beforeItem);
        Main.LOGGER.debug("Start: potionItem: " + potionItem);

        recruit.setItemInHand(InteractionHand.OFF_HAND, potionItem);
        recruit.startUsingItem(InteractionHand.OFF_HAND);
    }

    private boolean hasPotionInInv(){
        SimpleContainer inventory = recruit.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack itemStack = inventory.getItem(i);
            if (PotionUtils.getMobEffects(itemStack).size() > 0 && PotionUtils.getMobEffects(itemStack).stream().noneMatch(instance -> instance.getEffect().getCategory().equals(MobEffectCategory.HARMFUL))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ItemStack getPotionInInv(){
        SimpleContainer inventory = recruit.getInventory();
        ItemStack itemStack = null;
        for(int i = 0; i < inventory.getContainerSize(); i++){
            itemStack = inventory.getItem(i);
            if (PotionUtils.getMobEffects(itemStack).size() > 0 && PotionUtils.getMobEffects(itemStack).stream().noneMatch(instance -> instance.getEffect().getCategory().equals(MobEffectCategory.HARMFUL))) {
                return itemStack;
            }
        }
        return itemStack;
    }

    @Override
    public void tick() {
        super.tick();

        if(!recruit.isUsingItem() && recruit.getIsEating() && beforeItem != null) stop();
    }

    @Override
    public void stop() {
        recruit.setIsEating(false);
        potionItem.shrink(1);
        if(potionItem.getCount() == 1) potionItem.shrink(1);//fix infinite food?

        recruit.stopUsingItem();

        Main.LOGGER.debug("Stop: beforeFoodItem: " + beforeItem);
        Main.LOGGER.debug("Stop: foodStack: " + potionItem);

        resetItemInHand();
        recruit.eatCoolDown = 100;
    }

    public void resetItemInHand() {
        recruit.setItemInHand(InteractionHand.OFF_HAND, this.beforeItem);
        recruit.inventory.setItem(10, this.beforeItem);
    }
}
