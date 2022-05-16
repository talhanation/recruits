package com.talhanation.recruits.client.render;

import com.talhanation.recruits.client.models.ManModel;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;

public abstract class AbstractRecruitRenderer<E extends AbstractRecruitEntity> extends RecruitsBipedRenderer<E, ManModel<E>>{
    public AbstractRecruitRenderer(EntityRenderDispatcher mgr) {
        super(mgr, new ManModel(), new ManModel(0.5F, true), new ManModel(1.0F, true), 0.5F);
    }

}
