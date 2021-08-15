package com.talhanation.recruits.client.render;

import com.talhanation.recruits.client.models.ManModel;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;

public abstract class AbstractManRenderer<E extends AbstractRecruitEntity> extends RecruitsBipedRenderer<E, ManModel<E>>{
    public AbstractManRenderer(EntityRendererManager mgr) {
        super(mgr, new ManModel(), new ManModel(0.5F, true), new ManModel(1.0F, true), 0.5F);
    }
}
