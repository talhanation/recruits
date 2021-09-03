package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Hand;

public class RecruitUseShield extends Goal {
    public final AbstractRecruitEntity recruit;

    public RecruitUseShield(AbstractRecruitEntity recruit){
        this.recruit = recruit;
    }

    public boolean canUse() {
       return (this.recruit.getItemInHand(Hand.OFF_HAND).getItem().isShield(this.recruit.getItemInHand(Hand.OFF_HAND), this.recruit) && canRaiseShield());
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
            this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
        } else {
            this.recruit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public boolean canRaiseShield() {
        LivingEntity target = this.recruit.getTarget();
        if (target != null) {
            if (target instanceof IRangedAttackMob && target.distanceTo(this.recruit) >= 0.7D) {
                return true;
            }
            if (target.distanceTo(this.recruit) >= 0.5D && target.distanceTo(this.recruit) <= 1.5D) {
                return true;
            }
            recruit.stopUsingItem();
            return false;
        }
        recruit.stopUsingItem();
        return false;
    }
}
