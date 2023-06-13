package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.HorsemanEntity;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

import static com.talhanation.recruits.entities.HorsemanEntity.State.*;

public class HorsemanAttackAI extends Goal {
    private final HorsemanEntity horseman;
    private LivingEntity target;
    private HorsemanEntity.State state;
    private Vec3 movePos;

    public HorsemanAttackAI(HorsemanEntity recruit) {
        this.horseman = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        return horseman.getVehicle() instanceof RecruitHorseEntity && horseman.getFollowState() == 0 && !horseman.needsToGetFood() && !horseman.getShouldMount();
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
        switch (state) {
            case SELECT_TARGET -> {
                this.target = horseman.getTarget();
                if (target != null) {
                    Vec3 moveVec = target.position().subtract(horseman.position()).normalize();
                    Main.LOGGER.info("moveVec: " + moveVec.length());
                    if(moveVec.length() > 20){
                        moveVec.subtract(moveVec.scale(-2));
                    }
                    this.movePos = target.position().add(moveVec.scale(25D));
                    this.state = CHARGE_TARGET;
                }
            }

            case CHARGE_TARGET -> {

                if (target == null || !target.isAlive()) {
                    state = SELECT_TARGET;
                    return;
                }

                if (horseman.distanceToSqr(target) > 5F) {
                    horseman.getNavigation().moveTo(target.position().x, target.position().y, target.position().z, 1.15F);
                }
                else
                    state = MOVE_TO_POS;

                horseman.getLookControl().setLookAt(target, 30.0F, 30.0F);

                //Perform Attack
                if (horseman.distanceToSqr(target) < 5F) {
                    checkAndPerformAttack(target);
                }

                this.attackOthers();
            }

            case MOVE_TO_POS -> {
                if (target == null || !target.isAlive()) {
                    state = SELECT_TARGET;
                    return;
                }

                Vec3 movePos2 = new Vec3(movePos.x, horseman.position().y, movePos.z);
                horseman.getLookControl().setLookAt(movePos2);
                horseman.getNavigation().moveTo(movePos2.x, movePos2.y, movePos2.z, 1.15F);

                if (horseman.distanceToSqr(movePos2) < 6F) {
                    this.state = SELECT_TARGET;
                }

                this.attackOthers();
            }
        }
    }

    private void attackOthers() {
        List<LivingEntity> list = horseman.level.getEntitiesOfClass(LivingEntity.class, horseman.getBoundingBox().inflate(8D));
        for(LivingEntity entity : list){

            if (horseman.distanceToSqr(entity) < 5F) {
                if(horseman.canAttack(entity) && !entity.equals(horseman) && !entity.equals(target)){
                    checkAndPerformAttack(entity);
                }
            }
        }
    }

    protected void checkAndPerformAttack(LivingEntity target) {
        if(!horseman.swinging) {
            this.horseman.swing(InteractionHand.MAIN_HAND);
            this.horseman.doHurtTarget(target);
        }
    }
}
