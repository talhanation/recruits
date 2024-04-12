package com.talhanation.recruits.entities.ai.compat;

import com.talhanation.recruits.Main;
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

public class BlockTwoHandedWeapon extends Goal {
    public final PathfinderMob entity;

    public BlockTwoHandedWeapon(PathfinderMob recruit){
        this.entity = recruit;
    }

    public boolean canUse() {
        if(Main.isEpicKnightsLoaded){
            boolean noItemInOffhand = this.entity.getOffhandItem().isEmpty();
            if (entity instanceof AbstractRecruitEntity recruit){
                boolean forced = recruit.getShouldBlock();
                boolean normal = shouldBlock() && !recruit.isFollowing() && recruit.canBlock() && !recruit.getShouldMovePos();

                return (forced || normal) && noItemInOffhand && !this.entity.swinging;
            }
            else return noItemInOffhand && shouldBlock() && !this.entity.swinging;
        }
        return false;
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

    public boolean shouldBlock() {
        boolean isSelfTargeted = false;
        LivingEntity target = this.entity.getTarget();

        if (target != null && target.isAlive()) {
            Vec3 toTarget = this.entity.position().vectorTo(target.position());
            Vec3 forward = this.entity.getForward();
            if(forward.reverse().distanceToSqr(toTarget) < forward.distanceToSqr(toTarget)){
                return false;
            }

            if (target instanceof Mob mobTarget) {
                isSelfTargeted = mobTarget.getTarget() != null && mobTarget.getTarget().is(entity);
            }

            ItemStack itemStackInHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            double targetReach = AttackUtil.getAttackReachSqr(target);
            Item itemInHand = itemStackInHand.getItem();
            double distanceToTarget = this.entity.distanceToSqr(target);
            boolean isClose = this.entity instanceof HorsemanEntity horseman && horseman.isPassenger() ? distanceToTarget <= targetReach * (1.6) : distanceToTarget <= targetReach * (1.3) ;
            boolean isFar = distanceToTarget >= targetReach * 3;
            boolean inRange =  !isFar && distanceToTarget <= targetReach * (1.6);

            boolean isDanger = isSelfTargeted && itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand)
                    || itemInHand instanceof AxeItem
                    || itemInHand instanceof PickaxeItem
                    || itemInHand instanceof SwordItem;

            if (target instanceof RangedAttackMob && inRange && isSelfTargeted) {
                return true;
            }

            if ((isClose && (isSelfTargeted || target instanceof Player)) && (isDanger || (target instanceof Monster))){
                return true;
            }

            if (target.isBlocking() && inRange){
                return false;
            }

            if ( (itemInHand instanceof BowItem && !isClose) || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand) ) && inRange){
                return true;
            }
        }
        return false;
    }
}