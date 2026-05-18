package com.talhanation.recruits.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;



public class RecruitVillagerBiomeLayer extends RenderLayer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> {
    private static final int FULL_WHITE = 0xFFFFFFFF;

    private static final ResourceLocation[] BIOME_TEXTURE = {
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_desert.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_jungle.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_plains.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_savanna.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_snowy_tundra.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_swamp.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/biome/villager_taiga.png"),
    };

    public RecruitVillagerBiomeLayer(LivingEntityRenderer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> renderer) {
        super(renderer);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int p_117722_, AbstractRecruitEntity recruit, float p_117724_, float p_117725_, float p_117726_, float p_117727_, float p_117728_, float p_117729_) {
        if(!recruit.isInvisible()){
            this.getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutout(BIOME_TEXTURE[recruit.getBiome()])), p_117722_, OverlayTexture.NO_OVERLAY, FULL_WHITE);
        }
    }

}
