package com.talhanation.recruits.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

;


public class RecruitVillagerTeamColorLayer extends RenderLayer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_white.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_black.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_light_grey.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_grey.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_dark_grey.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_light_blue.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_blue.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_dark_blue.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_light_green.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_green.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_dark_green.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_light_red.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_red.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_dark_red.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_light_brown.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_brown.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_dark_brown.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_light_cyan.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_cyan.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_dark_cyan.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_yellow.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_orange.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_magenta.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_purple.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_team_gold.png")
    };
    private static final ResourceLocation TEXTURE2 = new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_base_cloth.png");

    public RecruitVillagerTeamColorLayer(LivingEntityRenderer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> renderer) {
        super(renderer);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int p_117722_, AbstractRecruitEntity recruit, float p_117724_, float p_117725_, float p_117726_, float p_117727_, float p_117728_, float p_117729_) {
        if(!recruit.isInvisible()){
            if (recruit.getTeam() != null) {
                this.getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE[recruit.getColor()])), p_117722_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            else{
                this.getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE2)), p_117722_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

}
