package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;

public class RecruitUseShield extends Goal {
    public final AbstractRecruitEntity recruit;

    public RecruitUseShield(AbstractRecruitEntity recruit){
        this.recruit = recruit;
    }

    public boolean canUse() {
       return (this.recruit.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.recruit.getItemInHand(Hand.OFF_HAND), this.recruit)
               && canRaiseShield()
               && !recruit.isFollowing()
               //&& recruit.canBlock
       );
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        if (this.recruit.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.recruit.getItemInHand(Hand.OFF_HAND), recruit)){
        this.recruit.startUsingItem(Hand.OFF_HAND);
        this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
        }
    }

    public  void stop(){
        this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    public void tick() {
        if (this.recruit.getUsedItemHand() == Hand.OFF_HAND) {
            this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        LivingEntity target = this.recruit.getTarget();

        if (target != null) {
            ItemStack itemStackinHand = target.getItemInHand(Hand.MAIN_HAND);
            Item itemInHand = itemStackinHand.getItem();
            boolean isClose = target.distanceTo(this.recruit) <= 3.75D;
            boolean isFar = target.distanceTo(this.recruit) >= 20.0D;
            boolean inRange =  !isFar && target.distanceTo(this.recruit) <= 15.0D;
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

            if ( (itemInHand instanceof BowItem && !isClose && target.isUsingItem()) || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackinHand) )  && inRange){
                return true;
            }

            recruit.stopUsingItem();
            return false;
        }
        recruit.stopUsingItem();
        return false;
    }
}