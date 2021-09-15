package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;

public class HorseAIRecruitRide extends Goal {

    private AbstractHorseEntity horse;
    private AbstractRecruitEntity recruit;
    private double speed;

    public HorseAIRecruitRide(AbstractHorseEntity horse, double speed) {
        this.horse = horse;
        this.speed = speed;
        this.recruit = (AbstractRecruitEntity) horse.getControllingPassenger();
    }

    @Override
    public boolean canUse() {
        if(horse.getControllingPassenger() instanceof AbstractRecruitEntity){
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        if (recruit.getOwner() != null)
            this.horse.getNavigation().moveTo(recruit.getOwner(), speed);
    }
}
