package com.talhanation.recruits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.models.RecruitHorseModel;
import com.talhanation.recruits.client.render.layer.HorseMarkingLayer;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RecruitHorseRenderer extends MobRenderer<RecruitHorseEntity, RecruitHorseModel<RecruitHorseEntity>> {
    private static final ResourceLocation[] TEXTURE = new ResourceLocation[]{
            new ResourceLocation("textures/entity/horse/horse_white.png"),
            new ResourceLocation("textures/entity/horse/horse_creamy.png"),
            new ResourceLocation("textures/entity/horse/horse_chestnut.png"),
            new ResourceLocation("textures/entity/horse/horse_brown.png"),
            new ResourceLocation("textures/entity/horse/horse_black.png"),
            new ResourceLocation("textures/entity/horse/horse_gray.png"),
            new ResourceLocation("textures/entity/horse/horse_darkbrown.png")};


    public RecruitHorseRenderer(EntityRendererProvider.Context manager) {
        super(manager, new RecruitHorseModel<>(manager.bakeLayer(ModelLayers.HORSE)), 1);
        this.addLayer(new HorseMarkingLayer(this));
    }

    public ResourceLocation getTextureLocation(RecruitHorseEntity horse) {
        return TEXTURE[horse.getTypeVariant()];
    }


    @Override
    public void render(@NotNull RecruitHorseEntity entity, float p_115456_, float p_115457_, PoseStack stack, MultiBufferSource bufferSource, int p_115460_) {
        stack.pushPose();
        stack.scale(-1.3F,-1.3F,1.3F);
        stack.popPose();
        super.render(entity, p_115456_, p_115457_, stack, bufferSource, p_115460_);
    }
}