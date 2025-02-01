package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.HorsemanEntity;
import com.talhanation.recruits.pathfinding.AsyncPathfinderMob;
import com.talhanation.recruits.util.AttackUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ToolActions;

public class UseShield extends Goal {
    public final AsyncPathfinderMob entity;

    public UseShield(AsyncPathfinderMob recruit){
        this.entity = recruit;
    }

    public boolean canUse() {
        boolean hasShield = this.entity.getOffhandItem().getItem().canPerformAction(entity.getOffhandItem(), ToolActions.SHIELD_BLOCK);
        if (entity instanceof AbstractRecruitEntity recruit){
            boolean forced = recruit.getShouldBlock();
            boolean normal = canRaiseShield() && !recruit.isFollowing() && recruit.canBlock() && !recruit.getShouldMovePos();

            return (forced || normal) && hasShield && !this.entity.swinging;
        }
        else return hasShield && canRaiseShield() && !this.entity.swinging;
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
        boolean isSelfTargeted = false;
        LivingEntity target = this.entity.getTarget();

        if (target != null && target.isAlive()) {

            if (target instanceof Mob mobTarget) {
                isSelfTargeted = mobTarget.getTarget() != null && mobTarget.getTarget().is(entity);
            }
            else if (target instanceof Player player){
                LivingEntity lastHurtMob = player.getLastHurtMob();
                isSelfTargeted = lastHurtMob != null && lastHurtMob.is(entity);
            }

            ItemStack itemStackInHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            double ownReach = AttackUtil.getAttackReachSqr(entity);
            Item itemInHand = itemStackInHand.getItem();
            double distanceToTarget = this.entity.distanceToSqr(target);
            boolean isTargetInReachToBlock = this.entity instanceof HorsemanEntity horseman && horseman.getVehicle() instanceof AbstractHorse ?  70 > distanceToTarget :  120 > distanceToTarget ;

            boolean isDanger = itemInHand instanceof AxeItem || itemInHand instanceof PickaxeItem || itemInHand instanceof SwordItem;

            if(isSelfTargeted){
                //For Ranged
                if(target instanceof RangedAttackMob || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand)) || (itemInHand instanceof BowItem && target.getTicksUsingItem() > 0)){
                    return distanceToTarget > ownReach * 1.5;
                }
                //For Melee
                else return (isDanger || target instanceof Monster) && isTargetInReachToBlock;
            }
        }
        return false;
    }
}