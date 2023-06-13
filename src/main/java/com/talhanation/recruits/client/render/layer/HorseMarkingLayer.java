package com.talhanation.recruits.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.client.models.RecruitHorseModel;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
@OnlyIn(Dist.CLIENT)
public class HorseMarkingLayer extends RenderLayer<RecruitHorseEntity, RecruitHorseModel<RecruitHorseEntity>> {
    private static final ResourceLocation[] TEXTURE = new ResourceLocation[]{
            new ResourceLocation("textures/entity/horse/horse_markings_white.png"),
            new ResourceLocation("textures/entity/horse/horse_markings_whitefield.png"),
            new ResourceLocation("textures/entity/horse/horse_markings_whitedots.png"),
            new ResourceLocation("textures/entity/horse/horse_markings_blackdots.png")};


    public HorseMarkingLayer(RenderLayerParent<RecruitHorseEntity, RecruitHorseModel<RecruitHorseEntity>> parent) {
        super(parent);
    }

    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int p_117060_, RecruitHorseEntity entity, float p_117062_, float p_117063_, float p_117064_, float p_117065_, float p_117066_, float p_117067_) {
        ResourceLocation resourcelocation = TEXTURE[entity.getTypeMarking()];
        if (resourcelocation != null && !entity.isInvisible()) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityTranslucent(resourcelocation));
            this.getParentModel().renderToBuffer(poseStack, vertexconsumer, p_117060_, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
