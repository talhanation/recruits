package com.talhanation.recruits.client.gui.component;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.List;

public class BannerRenderer {
    private final List<Pair<BannerPattern, DyeColor>> resultBannerPatterns;
    private final ModelPart flag;
    private final ItemStack bannerItem;
    private RecruitsTeam recruitsTeam;
    private Minecraft minecraft;
    public BannerRenderer(RecruitsTeam team) {
        this.bannerItem = ItemStack.of(team.getBanner());
        this.resultBannerPatterns = BannerBlockEntity.createPatterns(((BannerItem) this.bannerItem.getItem()).getColor(), BannerBlockEntity.getItemPatterns(this.bannerItem));
        this.flag = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
        this.recruitsTeam = team;
        minecraft = Minecraft.getInstance();
    }

    public void renderBanner(PoseStack poseStack, int left, int top, int width, int height, int scale0) {
        if (bannerItem != null) {
            GuiComponent.blit(poseStack, left, top, left + width, top + height, 0, 0, 256, 256);
            Lighting.setupForFlatItems();

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            poseStack.pushPose();
            poseStack.translate(left + 10, top + 20, 0.0D);
            poseStack.scale(scale0, -scale0, 1.0F);

            float scale = 0.6666667F;
            poseStack.scale(scale, -scale, -scale);
            this.flag.xRot = 0.0F;
            this.flag.y = -32.0F;

            net.minecraft.client.renderer.blockentity.BannerRenderer.renderPatterns(poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true, this.resultBannerPatterns);
            poseStack.popPose();
            bufferSource.endBatch();
        }
    }
}
