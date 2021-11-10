package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.Hand;

public class RecruitQuaffGoal extends Goal {

    AbstractRecruitEntity recruit;
    ItemStack potionItem;

    public RecruitQuaffGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        if (recruit.isUsingItem()) return false;
        if (recruit.beforeFoodItem != null) return false;

        return recruit.getTarget() != null /*&& recruit.getActiveEffects().stream().noneMatch(instance -> instance.getEffect().getCategory().equals(EffectType.BENEFICIAL)) /* Comment out to make the recruit not quaff another potion if it already has a positive effect */;
    }

    @Override
    public void start() {
        if (hasPotionInInv()) {
            recruit.beforeFoodItem = recruit.getItemInHand(Hand.OFF_HAND);

            recruit.setIsEating(true);
            recruit.setItemInHand(Hand.OFF_HAND, potionItem);
            recruit.setSlot(10, recruit.beforeFoodItem);

            recruit.startUsingItem(Hand.OFF_HAND);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean hasPotionInInv(){
        Inventory inventory = recruit.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack itemStack = inventory.getItem(i);
            if (PotionUtils.getMobEffects(itemStack).size() > 0 && PotionUtils.getMobEffects(itemStack).stream().noneMatch(instance -> instance.getEffect().getCategory().equals(EffectType.HARMFUL))) {
                potionItem = itemStack.copy();
                itemStack.shrink(1);

                return true;
            }
        }
        return false;
    }
}
