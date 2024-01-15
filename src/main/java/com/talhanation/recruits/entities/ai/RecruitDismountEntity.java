package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.List;

public class RecruitDismountEntity extends Goal {

    private final AbstractRecruitEntity recruit;
    private Entity mount;

    public RecruitDismountEntity(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.dismount > 0 && !this.recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start(){
        this.mount = recruit.getVehicle();
    }

    public void tick() {
        if(recruit.dismount > 0) recruit.dismount--;
        this.recruit.stopRiding();

        if(this.mount instanceof Boat && this.recruit.tickCount % 40 == 0)
            this.recruit.getJumpControl().jump();
    }
}