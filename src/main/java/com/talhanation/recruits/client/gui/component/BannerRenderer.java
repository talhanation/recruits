package com.talhanation.recruits.client.gui.component;

import com.mojang.blaze3d.platform.Lighting;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jetbrains.annotations.Nullable;

public class BannerRenderer {
    private BannerPatternLayers resultBannerPatterns;
    private DyeColor baseColor = DyeColor.WHITE;
    private final ModelPart flag;
    private ItemStack bannerItem;
    private RecruitsFaction recruitsFaction;
    private final Minecraft minecraft;
    public BannerRenderer(@Nullable RecruitsFaction team) {
        this.recruitsFaction = team;
        boolean fail = true;
        if (team != null && team.getBanner() != null){
            ItemStack itemStack = Minecraft.getInstance().level == null ? ItemStack.EMPTY : ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), team.getBanner());
            if(itemStack.getItem() instanceof BannerItem bannerItem1){
                this.bannerItem = itemStack;
                this.baseColor = bannerItem1.getColor();
                this.resultBannerPatterns = this.bannerItem.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
                fail = false;
            }

        }

        if(fail){
            this.bannerItem = ItemStack.EMPTY;
            this.resultBannerPatterns = BannerPatternLayers.EMPTY;
        }

        this.flag = Minecraft.getInstance().getEntityModels()
                .bakeLayer(ModelLayers.BANNER)
                .getChild("flag"); 
        this.minecraft = Minecraft.getInstance();
    }

    public void renderBanner(GuiGraphics guiGraphics, int left, int top, int width, int height, int scale0) {
        if (bannerItem.isEmpty() || this.flag == null || this.resultBannerPatterns == null) return;

        Lighting.setupForFlatItems();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(left + 10, top + 20, 0.0D);
            guiGraphics.pose().scale(scale0, -scale0, 1.0F);

            float scale = 0.6666667F;
            guiGraphics.pose().scale(scale, -scale, -scale);
            this.flag.xRot = 0.0F;
            this.flag.y = -32.0F;

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            net.minecraft.client.renderer.blockentity.BannerRenderer.renderPatterns(
                    guiGraphics.pose(), bufferSource, 15728880, OverlayTexture.NO_OVERLAY,
                    this.flag, Sheets.BANNER_BASE, true, this.baseColor, this.resultBannerPatterns);
            bufferSource.endBatch();
        } finally {
            guiGraphics.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }

    public void setBannerItem(ItemStack bannerItem) {
        if(bannerItem.getItem() instanceof BannerItem item){
            this.bannerItem = bannerItem;
            this.baseColor = item.getColor();
            this.resultBannerPatterns = this.bannerItem.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        }
    }

    public void setRecruitsFaction(RecruitsFaction faction){
        if(faction == null) return;
        this.recruitsFaction = faction;
        this.bannerItem = Minecraft.getInstance().level == null ? ItemStack.EMPTY : ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), faction.getBanner());
        if (this.bannerItem.getItem() instanceof BannerItem item) {
            this.baseColor = item.getColor();
            this.resultBannerPatterns = this.bannerItem.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        }
    }
}
