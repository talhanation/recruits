package com.talhanation.recruits.client.models;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class RecruitsBipedModel<E extends LivingEntity> extends HumanoidModel<E> {
    public boolean showChest = false;
    public RecruitsBipedModel(float modelSize) {
        super(modelSize);
    }

    public RecruitsBipedModel(float modelSize, float yOff, int texW, int texH) {
        super(modelSize, yOff, texW, texH);
    }

    public RecruitsBipedModel(Function<ResourceLocation, RenderType> renderType, float modelSize, float yOff, int texW, int texH) {
        super(renderType, modelSize, yOff, texW, texH);
    }

}
