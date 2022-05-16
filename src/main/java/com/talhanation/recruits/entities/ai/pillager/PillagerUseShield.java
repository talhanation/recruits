package com.talhanation.recruits.entities.ai.pillager;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.*;

/*
code by talhanation all rights reserved
contact: talhakantar@yahoo.com
 */

public class PillagerUseShield extends Goal {
    public final AbstractIllager pillager;

    public PillagerUseShield(AbstractIllager pillager){
        this.pillager = pillager;
    }

    public boolean canUse() {
       return (this.pillager.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ShieldItem
               && canRaiseShield()
       );
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        if (this.pillager.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ShieldItem){
        this.pillager.startUsingItem(InteractionHand.OFF_HAND);
        this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
        }
    }

    public  void stop(){
        this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    public void tick() {
        if (this.pillager.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.pillager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        LivingEntity target = this.pillager.getTarget();

        if (target != null) {
            ItemStack itemStackinHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            Item itemInHand = itemStackinHand.getItem();
            boolean isClose = target.distanceTo(this.pillager) <= 3.75D;
            boolean isFar = target.distanceTo(this.pillager) >= 20.0D;
            boolean inRange =  !isFar && target.distanceTo(this.pillager) <= 15.0D;
            boolean isDanger = itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackinHand) || itemInHand instanceof AxeItem || itemInHand instanceof PickaxeItem || itemInHand instanceof SwordItem;

            if (target instanceof RangedAttackMob && inRange ) {
                return true;
            }

            if (isClose && (isDanger || target instanceof Monster)) {
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