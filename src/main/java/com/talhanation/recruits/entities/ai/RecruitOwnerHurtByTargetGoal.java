package com.talhanation.recruits.entities.ai;

import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TargetGoal;
/*
public class RecruitOwnerHurtByTargetGoal extends TargetGoal {
    private final AbstractHoldingEntity recruit;
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public RecruitOwnerHurtByTargetGoal(AbstractHoldingEntity recruitEntity) {
        super(recruitEntity, false);
        this.recruit = recruitEntity;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        if (this.recruit.isOwned() && !this.recruit.isOrderedToHold()) {
            LivingEntity owner = this.recruit.getOwner();
            if (owner == null) {
                return false;
            } else {
                this.ownerLastHurtBy = owner.getLastHurtByMob();
                int i = owner.getLastHurtByMobTimestamp();
                return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, EntityPredicate.DEFAULT) && this.recruit.wantsToAttack(this.ownerLastHurtBy, owner);
            }
        } else {
            return false;
        }
    }

    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        LivingEntity owner = this.recruit.getOwner();
        if (owner != null) {
            this.timestamp = owner.getLastHurtByMobTimestamp();
        }

        super.start();
    }

}
*/
