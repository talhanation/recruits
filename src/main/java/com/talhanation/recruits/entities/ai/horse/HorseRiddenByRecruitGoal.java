package com.talhanation.recruits.entities.ai.horse;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public class HorseRiddenByRecruitGoal extends Goal {

    public final AbstractHorse horse;
    public HorseRiddenByRecruitGoal(AbstractHorse horse){
        this.horse = horse;
    }

    public boolean canUse() {
        return horse.getControllingPassenger() instanceof AbstractRecruitEntity;
    }

    @Override
    public void start() {
        super.start();
        double speed;
        if(this.horse.getPersistentData().contains("oldSpeed")){
            speed = horse.getPersistentData().getDouble("oldSpeed");
        }
        else{
            speed = this.horse.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
            this.horse.getPersistentData().putDouble("oldSpeed", speed);
        }
        this.horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.255  + speed);
    }

    @Override
    public void stop() {
        super.stop();
        double oldSpeed = horse.getPersistentData().getDouble("oldSpeed");
        this.horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(oldSpeed);
    }
}
