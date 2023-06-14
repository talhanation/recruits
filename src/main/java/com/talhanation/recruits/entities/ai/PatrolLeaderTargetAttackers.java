package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class PatrolLeaderTargetAttackers extends TargetGoal {
    private final AbstractRecruitEntity patrolLeader;
    private LivingEntity target;

    public PatrolLeaderTargetAttackers(AbstractRecruitEntity recruit) {
        super(recruit, false);
        this.patrolLeader = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        if(getPatrollersTarget() != null){
            this.target = getPatrollersTarget();
            return true;
        }
        else return false;
    }

    public void start() {
        this.mob.setTarget(this.target);
        this.mob.setLastHurtMob(this.target);

        super.start();
    }

    @Override
    public void tick() {
        super.tick();
    }
    @Nullable
    public LivingEntity getPatrollersTarget(){
        List<AbstractRecruitEntity> list = patrolLeader.level.getEntitiesOfClass(AbstractRecruitEntity.class, patrolLeader.getBoundingBox().inflate(32D));
        for(AbstractRecruitEntity patrols : list){
            if (patrols.getUUID().equals(patrolLeader.getProtectUUID()) && patrols.isAlive() && patrols.getTarget() != null){
                return patrols.getTarget();
            }
        }
        return null;
    }

}
