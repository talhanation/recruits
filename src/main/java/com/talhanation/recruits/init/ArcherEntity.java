package com.talhanation.recruits.init;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public class ArcherEntity extends BowmanEntity {

    public ArcherEntity(EntityType<? extends AbstractRecruitEntity> entityType, World world) {
        super(entityType, world);
    }
}
