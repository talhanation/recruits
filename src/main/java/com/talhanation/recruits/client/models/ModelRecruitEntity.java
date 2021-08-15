package com.talhanation.recruits.client.models;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.model.PlayerModel;

public class ModelRecruitEntity extends PlayerModel<AbstractRecruitEntity>{
    public ModelRecruitEntity(float modelSize, boolean smallArms) {
        super(modelSize, smallArms);
    }
}
