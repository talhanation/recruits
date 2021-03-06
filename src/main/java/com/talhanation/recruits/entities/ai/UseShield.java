package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ToolActions;

public class UseShield extends Goal {
    public final PathfinderMob entity;

    public UseShield(PathfinderMob recruit){
        this.entity = recruit;
    }

    public boolean canUse() {
        if (entity instanceof AbstractRecruitEntity recruit){
            return (this.entity.getOffhandItem().getItem().canPerformAction(entity.getOffhandItem(), ToolActions.SHIELD_BLOCK)
                    && canRaiseShield()
                    && !recruit.isFollowing()
                    && recruit.canBlock()
            );
        }
        else return (this.entity.getOffhandItem().getItem().canPerformAction(entity.getOffhandItem(), ToolActions.SHIELD_BLOCK)
               && canRaiseShield()
       );
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        if (this.entity.getOffhandItem().getItem().canPerformAction(entity.getOffhandItem(), ToolActions.SHIELD_BLOCK)){
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
            boolean isClose = target.distanceTo(this.entity) <= 2.75D;
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