package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.HorsemanEntity;
import com.talhanation.recruits.util.AttackUtil;
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
import net.minecraft.world.phys.Vec3;
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
            Vec3 toTarget = this.entity.position().vectorTo(target.position());
            Vec3 forward = this.entity.getForward();
            if(forward.reverse().distanceToSqr(toTarget) < forward.distanceToSqr(toTarget) * 1.2){
                return false;
            }

            if (target instanceof Mob mobTarget) {
                isSelfTargeted = mobTarget.getTarget() != null && mobTarget.getTarget().is(entity);
            }

            ItemStack itemStackInHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            double targetReach = AttackUtil.getAttackReachSqr(target);
            Item itemInHand = itemStackInHand.getItem();
            double distanceToTarget = this.entity.distanceToSqr(target);
            boolean isClose = this.entity instanceof HorsemanEntity horseman && horseman.isPassenger() ? distanceToTarget <= targetReach * 1.5 : distanceToTarget <= targetReach * 1.25 ;

            boolean isDanger = isSelfTargeted && itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand)
                    || itemInHand instanceof AxeItem
                    || itemInHand instanceof PickaxeItem
                    || itemInHand instanceof SwordItem;

            if (target instanceof RangedAttackMob && isClose && isSelfTargeted) {
                return true;
            }

            if ((isClose && (isSelfTargeted || target instanceof Player)) && (isDanger || (target instanceof Monster))){
                return true;
            }

            if (target.isBlocking() && isClose){
                return false;
            }

            if ( (itemInHand instanceof BowItem && !isClose) || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand) ) && isClose){
                return true;
            }
        }
        return false;
    }
}