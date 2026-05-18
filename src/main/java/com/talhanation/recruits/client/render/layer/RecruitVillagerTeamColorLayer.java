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

;


public class RecruitVillagerTeamColorLayer extends RenderLayer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> {
    private static final int FULL_WHITE = 0xFFFFFFFF;

    private static final ResourceLocation[] TEXTURE = {
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_white.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_black.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_light_grey.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_grey.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_dark_grey.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_light_blue.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_blue.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_dark_blue.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_light_green.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_green.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_dark_green.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_light_red.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_red.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_dark_red.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_light_brown.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_brown.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_dark_brown.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_light_cyan.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_cyan.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_dark_cyan.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_yellow.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_orange.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_magenta.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_purple.png"),
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_team_gold.png")
    };
    private static final ResourceLocation TEXTURE2 = ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/villager/villager_base_cloth.png");

    public RecruitVillagerTeamColorLayer(LivingEntityRenderer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> renderer) {
        super(renderer);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int p_117722_, AbstractRecruitEntity recruit, float p_117724_, float p_117725_, float p_117726_, float p_117727_, float p_117728_, float p_117729_) {
        if(!recruit.isInvisible()){
            if (recruit.getTeam() != null) {
                this.getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE[recruit.getColor()])), p_117722_, OverlayTexture.NO_OVERLAY, FULL_WHITE);
            }
            else{
                this.getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE2)), p_117722_, OverlayTexture.NO_OVERLAY, FULL_WHITE);
            }
        }
    }

}
