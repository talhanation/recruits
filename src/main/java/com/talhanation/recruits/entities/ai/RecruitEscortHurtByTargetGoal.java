package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class RecruitEscortHurtByTargetGoal extends TargetGoal {
    private final AbstractRecruitEntity recruit;
    private LivingEntity escortLastHurtBy;
    private int timestamp;

    public RecruitEscortHurtByTargetGoal(AbstractRecruitEntity recruit) {
        super(recruit, false);
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        if(recruit.getShouldEscort()) {
            LivingEntity escort = this.recruit.getEscort();
            if (escort == null) {
                return false;
            } else {
                this.escortLastHurtBy = escort.getLastHurtByMob();
                int i = escort.getLastHurtByMobTimestamp();
                return i != this.timestamp
                        && this.canAttack(this.escortLastHurtBy, TargetingConditions.DEFAULT)
                        && this.recruit.wantsToAttack(this.escortLastHurtBy, escort)
                        && recruit.getState() != 3;
            }
        }
        else
            return false;
    }

    public void start() {
        this.mob.setTarget(this.escortLastHurtBy);
        this.mob.setLastHurtMob(this.escortLastHurtBy);
        LivingEntity livingentity = this.recruit.getEscort();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtByMobTimestamp();
        }
        super.start();
    }



}