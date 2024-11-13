package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.theading.RecruitsAsyncGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;

public class RecruitDismountAsyncGoal extends RecruitsAsyncGoal {
    private final AbstractRecruitEntity recruit;
    private Entity mount;

    public RecruitDismountAsyncGoal(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
    }


    public boolean canUse() {
        return recruit.dismount > 0 && !this.recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        this.mount = recruit.getVehicle();
    }

    public void tick() {
        // Asynchrone Tick-Logik, die unabhängig vom Haupt-Thread läuft
        if (recruit.dismount > 0) recruit.dismount--;
        this.recruit.stopRiding();

        if (this.mount instanceof Boat && this.recruit.tickCount % 40 == 0) {
            this.recruit.getJumpControl().jump();
        }
    }
}
