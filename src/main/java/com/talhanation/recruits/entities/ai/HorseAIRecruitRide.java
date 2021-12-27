package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;

public class HorseAIRecruitRide extends Goal {

    private CreatureEntity horse;
    private AbstractRecruitEntity recruit;
    private double speed;

    public HorseAIRecruitRide(CreatureEntity horse, double speed) {
        this.horse = horse;
        this.speed = speed;
        this.recruit = (AbstractRecruitEntity) horse.getControllingPassenger();
    }

    @Override
    public boolean canUse() {
        return horse.getControllingPassenger() instanceof AbstractRecruitEntity;
    }

    @Override
    public void tick() {
        System.out.println("PATHHHHHH");
        this.horse.getNavigation().moveTo(this.recruit.getNavigation().getPath(), speed);
    }
}
