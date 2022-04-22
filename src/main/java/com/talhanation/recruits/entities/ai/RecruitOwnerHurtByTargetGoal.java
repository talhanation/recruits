package com.talhanation.recruits.entities.ai;

import java.util.EnumSet;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;

public class RecruitOwnerHurtByTargetGoal extends TargetGoal {
    private final AbstractRecruitEntity recruit;
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public RecruitOwnerHurtByTargetGoal(AbstractRecruitEntity recruit) {
        super(recruit, false);
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        if (this.recruit.isTame() && !this.recruit.isOrderedToSit()) {
            LivingEntity livingentity = this.recruit.getOwner();
            if (livingentity == null) {
                return false;
            } else {
                this.ownerLastHurtBy = livingentity.getLastHurtByMob();
                int i = livingentity.getLastHurtByMobTimestamp();
                return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, EntityPredicate.DEFAULT) && this.recruit.wantsToAttack(this.ownerLastHurtBy, livingentity) && recruit.getState() != 3;
            }
        } else {
            return false;
        }
    }

    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        LivingEntity livingentity = this.recruit.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtByMobTimestamp();
        }

        super.start();
    }
}