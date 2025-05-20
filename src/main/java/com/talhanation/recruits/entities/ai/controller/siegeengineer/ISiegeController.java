package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface ISiegeController {

     void tryMount(Entity entity);
     void tryDismount();
     void tick();
     boolean updateAttacking();
     void reset();
     Entity getSiegeEntity();

    void calculatePath();

    Vec3 getTargetPos();
    void setTargetPos(Vec3 vec3);
}
