package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ToolActions;

public class UseShield extends Goal {
    public final PathfinderMob entity;

    public UseShield(PathfinderMob recruit){
        this.entity = recruit;
    }

    public boolean canUse() {
        boolean hasShield = this.entity.getOffhandItem().getItem().canPerformAction(entity.getOffhandItem(), ToolActions.SHIELD_BLOCK);
        if (entity instanceof AbstractRecruitEntity recruit){
            boolean forced = recruit.getShouldBlock();
            boolean normal = canRaiseShield() && !recruit.isFollowing() && recruit.canBlock();

            return (forced || normal) && hasShield;
        }
        else return hasShield && canRaiseShield();
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
        entity.stopUsingItem();
    }

    public void tick() {
        if (this.entity.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        boolean isSelfTarget = false;
        LivingEntity target = this.entity.getTarget();
        if (target instanceof Mob mobTarget) {
            isSelfTarget = mobTarget.getTarget() != null && mobTarget.getTarget().is(entity);
        }
        if (target != null && target.isAlive() && !this.entity.swinging) {
            ItemStack itemStackinHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            Item itemInHand = itemStackinHand.getItem();
            boolean isClose = target.distanceTo(this.entity) <= 3.75D;
            boolean isFar = target.distanceTo(this.entity) >= 20.0D;
            boolean inRange =  !isFar && target.distanceTo(this.entity) <= 15.0D;

            boolean isDanger = isSelfTarget && itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackinHand) || itemInHand instanceof AxeItem || itemInHand instanceof PickaxeItem || itemInHand instanceof SwordItem;

            if (target instanceof RangedAttackMob && inRange && isSelfTarget) {
                return true;
            }

            if ((isClose && (isSelfTarget || target instanceof Player)) && (isDanger || (target instanceof Monster))){
                return true;
            }

            if (target.isBlocking() && inRange){
                return false;
            }

            if ( (itemInHand instanceof BowItem && !isClose) || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackinHand) ) && inRange){
                return true;
            }
        }
        return false;
    }
}