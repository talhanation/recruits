package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.item.*;
import net.minecraft.world.InteractionHand;

import javax.swing.*;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;

public class UseShield extends Goal {
    public final PathfinderMob entity;
    public int usedTimer;

    public UseShield(PathfinderMob recruit){
        this.entity = recruit;
    }

    public boolean canUse() {
        if (entity instanceof AbstractRecruitEntity){
            AbstractRecruitEntity recruit = (AbstractRecruitEntity) entity;
            return (recruit.getItemInHand(InteractionHand.OFF_HAND).getItem().isShield(this.entity.getItemInHand(InteractionHand.OFF_HAND), this.entity)
                    && canRaiseShield()
                    && !recruit.isFollowing()
            );
        }
        else return (this.entity.getItemInHand(InteractionHand.OFF_HAND).getItem().isShield(this.entity.getItemInHand(InteractionHand.OFF_HAND), this.entity)
               && canRaiseShield()

       );
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        if (this.entity.getItemInHand(InteractionHand.OFF_HAND).getItem().isShield(this.entity.getItemInHand(InteractionHand.OFF_HAND), entity)){
        this.entity.startUsingItem(InteractionHand.OFF_HAND);
        this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
        }
    }

    public  void stop(){
        this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    public void tick() {
        if (this.entity.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        LivingEntity target = this.entity.getTarget();

        if (target != null) {
            ItemStack itemStackinHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            Item itemInHand = itemStackinHand.getItem();
            boolean isClose = target.distanceTo(this.entity) <= 3.75D;
            boolean isFar = target.distanceTo(this.entity) >= 20.0D;
            boolean inRange =  !isFar && target.distanceTo(this.entity) <= 15.0D;
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

            entity.stopUsingItem();
            return false;
        }
        entity.stopUsingItem();
        return false;
    }
}