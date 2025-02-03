package com.talhanation.recruits.client.gui.component;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Holder;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BannerRenderer {
    private List<Pair<Holder<BannerPattern>, DyeColor>> resultBannerPatterns;
    private final ModelPart flag;
    private ItemStack bannerItem;
    private RecruitsTeam recruitsTeam;
    private final Minecraft minecraft;
    public BannerRenderer(@Nullable RecruitsTeam team) {
        this.recruitsTeam = team;
        boolean fail = true;
        if (team != null && team.getBanner() != null){
            ItemStack itemStack = ItemStack.of(team.getBanner());
            if(itemStack.getItem() instanceof BannerItem bannerItem1){
                this.bannerItem = itemStack;
                this.resultBannerPatterns = BannerBlockEntity.createPatterns(
                        bannerItem1.getColor(),
                        BannerBlockEntity.getItemPatterns(this.bannerItem)
                );
                fail = false;
            }

        }

        if(fail){
            this.bannerItem = ItemStack.EMPTY;
            this.resultBannerPatterns = List.of();
        }

        this.flag = Minecraft.getInstance().getEntityModels()
                .bakeLayer(ModelLayers.BANNER)
                .getChild("flag"); 
        this.minecraft = Minecraft.getInstance();
    }

    public void renderBanner(GuiGraphics guiGraphics, int left, int top, int width, int height, int scale0) {
        if (!bannerItem.isEmpty()) {
            //guiGraphics.blit(left, top, left + width, top + height, 0, 0, 256, 256, null);
            Lighting.setupForFlatItems();

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + 10, top + 20, 0.0D);
            guiGraphics.pose().scale(scale0, -scale0, 1.0F);

            float scale = 0.6666667F;
            guiGraphics.pose().scale(scale, -scale, -scale);
            this.flag.xRot = 0.0F;
            this.flag.y = -32.0F;

            net.minecraft.client.renderer.blockentity.BannerRenderer.renderPatterns(guiGraphics.pose(), bufferSource, 15728880, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true, this.resultBannerPatterns);
            guiGraphics.pose().popPose();
            bufferSource.endBatch();
        }
    }

    public void setBannerItem(ItemStack bannerItem) {
        if(bannerItem.getItem() instanceof BannerItem){
            this.bannerItem = bannerItem;
            this.resultBannerPatterns = BannerBlockEntity.createPatterns(((BannerItem) this.bannerItem.getItem()).getColor(), BannerBlockEntity.getItemPatterns(this.bannerItem));
        }
    }

    public void setRecruitsTeam(RecruitsTeam team){
        this.recruitsTeam = team;
        this.bannerItem = ItemStack.of(team.getBanner());
        this.resultBannerPatterns = BannerBlockEntity.createPatterns(((BannerItem) this.bannerItem.getItem()).getColor(), BannerBlockEntity.getItemPatterns(this.bannerItem));
    }
}
