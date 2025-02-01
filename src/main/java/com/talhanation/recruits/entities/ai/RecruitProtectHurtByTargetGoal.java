package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;

public class RecruitProtectHurtByTargetGoal extends TargetGoal {
    private final AbstractRecruitEntity recruit;
    private LivingEntity protectLastHurtBy;
    private int timestamp;

    public RecruitProtectHurtByTargetGoal(AbstractRecruitEntity recruit) {
        super(recruit, false);
        this.recruit = recruit;
    }

    public boolean canUse() {
        if(recruit.getShouldProtect()) {
            LivingEntity protectingMob = this.recruit.getProtectingMob();
            if (protectingMob == null) {
                return false;
            } else {
                this.protectLastHurtBy = protectingMob.getLastHurtByMob();
                int i = protectingMob.getLastHurtByMobTimestamp();
                return i != this.timestamp
                        && this.canAttack(this.protectLastHurtBy, TargetingConditions.DEFAULT)
                        && RecruitEvents.canAttack(protectingMob, this.protectLastHurtBy)
                        && recruit.getState() != 3;
            }
        }
        else
            return false;
    }

    public void start() {
        this.mob.setTarget(this.protectLastHurtBy);
        this.mob.setLastHurtMob(this.protectLastHurtBy);
        LivingEntity livingentity = this.recruit.getProtectingMob();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtByMobTimestamp();
        }
        super.start();
    }



}