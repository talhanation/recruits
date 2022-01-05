package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;

import javax.swing.*;

public class UseShield extends Goal {
    public final CreatureEntity entity;
    public int usedTimer;

    public UseShield(CreatureEntity recruit){
        this.entity = recruit;
    }

    public boolean canUse() {
        if (entity instanceof AbstractRecruitEntity){
            AbstractRecruitEntity recruit = (AbstractRecruitEntity) entity;
            return (recruit.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.entity.getItemInHand(Hand.OFF_HAND), this.entity)
                    && canRaiseShield()
                    && !recruit.isFollowing()
            );
        }
        else return (this.entity.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.entity.getItemInHand(Hand.OFF_HAND), this.entity)
               && canRaiseShield()

       );
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        if (this.entity.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.entity.getItemInHand(Hand.OFF_HAND), entity)){
        this.entity.startUsingItem(Hand.OFF_HAND);
        this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
        }
    }

    public  void stop(){
        this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    public void tick() {
        if (this.entity.getUsedItemHand() == Hand.OFF_HAND) {
            this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        LivingEntity target = this.entity.getTarget();

        if (target != null) {
            ItemStack itemStackinHand = target.getItemInHand(Hand.MAIN_HAND);
            Item itemInHand = itemStackinHand.getItem();
            boolean isClose = target.distanceTo(this.entity) <= 3.75D;
            boolean isFar = target.distanceTo(this.entity) >= 20.0D;
            boolean inRange =  !isFar && target.distanceTo(this.entity) <= 15.0D;
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

            entity.stopUsingItem();
            return false;
        }
        entity.stopUsingItem();
        return false;
    }
}