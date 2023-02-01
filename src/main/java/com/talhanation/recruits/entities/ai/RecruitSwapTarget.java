package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class RecruitSwapTarget extends Goal {

    private final AbstractRecruitEntity recruit;
    private LivingEntity target;

    public RecruitSwapTarget(AbstractRecruitEntity recruit){
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        if(recruit.getTarget() != null)
            target = recruit.getTarget();
    }

    @Override
    public void tick() {
        super.tick();
        if(target != null){
            LivingEntity hurtingMob = recruit.getLastHurtByMob();//getting the mob that hurt the recruit

            if(hurtingMob !=null && !hurtingMob.equals(target)){
                recruit.setTarget(hurtingMob);
                if(hurtingMob.isDeadOrDying() && target != null){
                    recruit.setTarget(target);
                }
            }
        }
    }
}
