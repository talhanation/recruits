package com.talhanation.recruits.client.render;

import com.talhanation.recruits.client.models.ManModel;
import com.talhanation.recruits.entities.AbstractOrderAbleEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;

public abstract class AbstractAssassinRenderer<E extends AbstractOrderAbleEntity> extends RecruitsBipedRenderer<E, ManModel<E>>{
    public AbstractAssassinRenderer(EntityRendererManager mgr) {
        super(mgr, new ManModel(), new ManModel(0.5F, true), new ManModel(1.0F, true), 0.5F);
    }
}
