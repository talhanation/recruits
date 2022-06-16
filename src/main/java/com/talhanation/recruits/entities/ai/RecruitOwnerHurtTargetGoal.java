package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;

public class RecruitOwnerHurtTargetGoal extends TargetGoal {
    private final AbstractRecruitEntity recruitEntity;
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public RecruitOwnerHurtTargetGoal(AbstractRecruitEntity p_26114_) {
        super(p_26114_, false);
        this.recruitEntity = p_26114_;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        if (this.recruitEntity.isOwned()) {
            LivingEntity livingentity = this.recruitEntity.getOwner();
            if (livingentity == null) {
                return false;
            } else {
                this.ownerLastHurt = livingentity.getLastHurtMob();
                int i = livingentity.getLastHurtMobTimestamp();
                return i != this.timestamp && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT) && this.recruitEntity.wantsToAttack(this.ownerLastHurt, livingentity);
            }
        } else {
            return false;
        }
    }

    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        LivingEntity livingentity = this.recruitEntity.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtMobTimestamp();
        }

        super.start();
    }
}