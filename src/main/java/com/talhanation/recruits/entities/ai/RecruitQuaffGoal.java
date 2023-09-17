package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.InteractionHand;

import javax.annotation.Nullable;

public class RecruitQuaffGoal extends Goal {

    public AbstractRecruitEntity recruit;
    public ItemStack potionItem;
    public ItemStack beforeItem;
    public int slotID;

    public RecruitQuaffGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return hasPotionInInv() && recruit.needsToPotion() && !recruit.isUsingItem();
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
        this.potionItem = getPotionInInvAndRemove();

        recruit.setItemInHand(InteractionHand.OFF_HAND, potionItem.copy());
        recruit.startUsingItem(InteractionHand.OFF_HAND);
        recruit.inventory.addItem(Items.GLASS_BOTTLE.getDefaultInstance());
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
    private ItemStack getPotionInInvAndRemove(){
        SimpleContainer inventory = recruit.getInventory();
        ItemStack itemStack = null;
        for(int i = 0; i < inventory.getContainerSize(); i++){
            itemStack = inventory.getItem(i);
            if (PotionUtils.getMobEffects(itemStack).size() > 0 && PotionUtils.getMobEffects(itemStack).stream().noneMatch(instance -> instance.getEffect().getCategory().equals(MobEffectCategory.HARMFUL))) {
                slotID = i;
                recruit.inventory.removeItemNoUpdate(i);
                return itemStack;
            }
        }
        return itemStack;
    }

    @Override
    public void stop() {
        recruit.stopUsingItem();

        resetItemInHand();
    }

    public void resetItemInHand() {
        recruit.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        recruit.inventory.setItem(4, ItemStack.EMPTY);

        recruit.setItemInHand(InteractionHand.OFF_HAND, this.beforeItem.copy());
    }
}
