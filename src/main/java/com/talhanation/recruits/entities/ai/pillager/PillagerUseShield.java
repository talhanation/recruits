package com.talhanation.recruits.entities.ai.pillager;

import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
/*
code by talhanation all rights reserved
contact: talhakantar@yahoo.com
 */

public class PillagerUseShield extends Goal {
    public final AbstractIllagerEntity pillager;

    public PillagerUseShield(AbstractIllagerEntity pillager){
        this.pillager = pillager;
    }

    public boolean canUse() {
       return (this.pillager.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.pillager.getItemInHand(Hand.OFF_HAND), this.pillager)
               && canRaiseShield()
       );
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        if (this.pillager.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.pillager.getItemInHand(Hand.OFF_HAND), pillager)){
        this.pillager.startUsingItem(Hand.OFF_HAND);
        this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
        }
    }

    public  void stop(){
        this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    public void tick() {
        if (this.pillager.getUsedItemHand() == Hand.OFF_HAND) {
            this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        LivingEntity target = this.pillager.getTarget();

        if (target != null) {
            ItemStack itemStackinHand = target.getItemInHand(Hand.MAIN_HAND);
            Item itemInHand = itemStackinHand.getItem();
            boolean isClose = target.distanceTo(this.pillager) <= 3.75D;
            boolean isFar = target.distanceTo(this.pillager) >= 20.0D;
            boolean inRange =  !isFar && target.distanceTo(this.pillager) <= 15.0D;
            boolean isDanger = itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackinHand) || itemInHand instanceof AxeItem || itemInHand instanceof PickaxeItem || itemInHand instanceof SwordItem;

            if (target instanceof IRangedAttackMob && inRange ) {
                return true;
            }

            if (isClose && (isDanger || target instanceof MonsterEntity)) {
                return true;
            }

            if (target.isBlocking() && inRange){
                return false;
            }

            if ( (itemInHand instanceof BowItem && !isClose) || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackinHand) )  && inRange){
                return true;
            }

            pillager.stopUsingItem();
            return false;
        }
        pillager.stopUsingItem();
        return false;
    }
}