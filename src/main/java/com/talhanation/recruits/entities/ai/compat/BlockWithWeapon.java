package com.talhanation.recruits.entities.ai.compat;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.HorsemanEntity;
import com.talhanation.recruits.util.AttackUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public class BlockWithWeapon extends Goal {
    public final AbstractRecruitEntity recruit;

    public BlockWithWeapon(AbstractRecruitEntity recruit){
        this.recruit = recruit;
    }

    public boolean canUse() {
        if(Main.isEpicKnightsLoaded && recruit.blockCoolDown == 0){
            boolean noItemInOffhand = this.recruit.getOffhandItem().isEmpty();
            boolean canBlockWithItem = this.recruit.getMainHandItem().getDescriptionId().contains("magistu");

            return canBlockWithItem && shouldBlock() && !recruit.isFollowing() && recruit.canBlock() && !recruit.getShouldMovePos() && noItemInOffhand && !this.recruit.swinging;
        }
        return false;
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        this.recruit.startUsingItem(InteractionHand.MAIN_HAND);
        this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
    }

    public  void stop(){
        this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.recruit.stopUsingItem();
    }

    public void tick() {
        if (this.recruit.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.16D);
        } else {
            this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean shouldBlock() {
        boolean isSelfTargeted = false;
        LivingEntity target = this.recruit.getTarget();

        if (target != null && target.isAlive()) {
            Vec3 toTarget = this.recruit.position().vectorTo(target.position());
            Vec3 forward = this.recruit.getForward();
            if(forward.reverse().distanceToSqr(toTarget) < forward.distanceToSqr(toTarget)){
                return false;
            }

            if (target instanceof Mob mobTarget) {
                isSelfTargeted = mobTarget.getTarget() != null && mobTarget.getTarget().is(recruit);
            }

            ItemStack itemStackInHand = target.getItemInHand(InteractionHand.MAIN_HAND);
            double targetReach = AttackUtil.getAttackReachSqr(target);
            Item itemInHand = itemStackInHand.getItem();
            double distanceToTarget = this.recruit.distanceToSqr(target);

            boolean isClose = this.recruit instanceof HorsemanEntity horseman && horseman.isPassenger() ? distanceToTarget <= targetReach * (1.2) : distanceToTarget <= targetReach * (1.1) ;
            boolean isFar = distanceToTarget >= targetReach * 3;
            boolean inRange =  !isFar && distanceToTarget <= targetReach * (1.2);

            boolean isDanger = isSelfTargeted && itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand)
                    || itemInHand instanceof AxeItem
                    || itemInHand instanceof PickaxeItem
                    || itemInHand instanceof SwordItem;

            if ((isClose && (isSelfTargeted || target instanceof Player)) && (isDanger || (target instanceof Monster))){
                return true;
            }

            if (target.isBlocking() && inRange){
                return false;
            }

            if ( (itemInHand instanceof BowItem && !isClose) || (itemInHand instanceof CrossbowItem && CrossbowItem.isCharged(itemStackInHand) ) && inRange){
                return false;
            }
        }
        return false;
    }
}