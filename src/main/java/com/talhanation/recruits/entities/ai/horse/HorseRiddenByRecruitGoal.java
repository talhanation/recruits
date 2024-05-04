package com.talhanation.recruits.entities.ai.horse;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public class HorseRiddenByRecruitGoal extends Goal {

    public final AbstractHorse horse;
    public boolean speedApplied;
    public boolean leaderFastSpeed;
    public HorseRiddenByRecruitGoal(AbstractHorse horse){
        this.horse = horse;
    }

    public boolean canUse() {
        return horse.getControllingPassenger() instanceof AbstractRecruitEntity;
    }

    @Override
    public void start() {
        super.start();
        speedApplied = false;
    }

    private void applyHorseSpeed() {
        double speed;
        if(this.horse.getPersistentData().contains("oldSpeed")){
            speed = horse.getPersistentData().getDouble("oldSpeed");
        }
        else{
            speed = this.horse.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
            this.horse.getPersistentData().putDouble("oldSpeed", speed);
        }
        if(this.horse.getControllingPassenger() instanceof AbstractLeaderEntity leader){
            leaderFastSpeed = leader.getFastPatrolling();
            double newSpeed = leaderFastSpeed ? speed : speed * 0.7D;
            this.horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.255  + newSpeed);
        }
        else
            this.horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.255  + speed);
    }


    @Override
    public void tick() {
        if(!speedApplied || this.horse.getControllingPassenger() instanceof AbstractLeaderEntity leader && leaderFastSpeed != leader.getFastPatrolling()){
            applyHorseSpeed();
            speedApplied = true;
        }

        //apply movement to horse when in water
        if(this.horse.isInWater()){
            this.horse.setDeltaMovement(this.horse.getDeltaMovement().add(0, 0.075, 0));
        }
    }


    @Override
    public void stop() {
        super.stop();
        double oldSpeed = horse.getPersistentData().getDouble("oldSpeed");
        this.horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(oldSpeed);
    }
}
