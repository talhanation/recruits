package com.talhanation.recruits.entities.ai.controller;

import net.minecraft.world.phys.Vec3;

public interface IAttackController {
    void tick();
    void setInitPos(Vec3 pos);
    boolean isTargetInRange();
}
