package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class RecruitDodgeGoal extends Goal {
    private LivingEntity target;
    private final AbstractRecruitEntity recruit;
    private int timer;

    public RecruitDodgeGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.getTarget() != null;
    }

    @Override
    public void start() {
        super.start();
        timer = 10;
        target =  recruit.getTarget();
    }

    @Override
    public void tick() {
        super.tick();
        if(target == null) return;

        if(recruit.position().distanceTo(target.position()) < 10){
            if(timer > 0) timer--;
            else this.doDodge();
        }
    }

    private void doDodge() {
        Vec3 toTarget = target.position().subtract(recruit.position()).normalize();
        Vec3 forward = recruit.getForward().normalize();
        Vec3 dodge = forward.cross(toTarget).normalize();

        int i = recruit.getRandom().nextInt(3);
        switch (i) {
            case 0 -> dodge = dodge.yRot(90);
            case 1 -> dodge = dodge.yRot(-90);
            case 2 -> dodge = dodge.yRot(-180);
        }

        recruit.setDeltaMovement(dodge.x, recruit.getDeltaMovement().y, dodge.z);
        this.timer = 10;
    }
}
