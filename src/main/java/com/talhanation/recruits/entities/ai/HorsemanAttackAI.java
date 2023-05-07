package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.HorsemanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static com.talhanation.recruits.entities.HorsemanEntity.State.*;

public class HorsemanAttackAI extends Goal {
    private final HorsemanEntity horseman;
    private LivingEntity target;
    private HorsemanEntity.State state;

    public HorsemanAttackAI(HorsemanEntity recruit) {
        this.horseman = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        return horseman.getFollowState() == 0 && !horseman.needsToGetFood() && !horseman.getShouldMount();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start() {
        super.start();
        this.target = horseman.getTarget();
        this.state = SELECT_TARGET;
    }

    public void tick() {
        Main.LOGGER.info("State: " + state);

        switch (state) {
            case SELECT_TARGET -> {
                this.target = horseman.getTarget();
                if (target != null) {

                    this.state = CHARGE_TARGET;
                }
            }

            case CHARGE_TARGET -> {
                Vec3 toTarget;
                if (target == null || !target.isAlive()) {
                    state = SELECT_TARGET;
                    return;
                }

                toTarget = target.position().subtract(horseman.position()).normalize();
                Vec3 moveVec = toTarget;
                Vec3 movePos = target.position().add(moveVec.scale(20D));

                if (horseman.distanceToSqr(movePos) > 3F)
                    horseman.getNavigation().moveTo(movePos.x, movePos.y, movePos.z, 1.1F);

                horseman.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
        //attack
    }

}
