package com.talhanation.recruits.client.models;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

public class RecruitsBipedModel <M extends LivingEntity> extends BipedModel {
    public RecruitsBipedModel(float size) {
        super(size);
    }

    protected RecruitsBipedModel(float size, float yOff, int texW, int texH) {
        super(size, yOff, texW, texH);
    }

    public RecruitsBipedModel(Function<ResourceLocation, RenderType> renderType, float size, float yOff, int texW, int texH) {
        super(renderType, size, yOff, texW, texH);
    }


}