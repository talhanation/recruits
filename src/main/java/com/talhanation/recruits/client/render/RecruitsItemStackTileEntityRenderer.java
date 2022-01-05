package com.talhanation.recruits.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.client.models.HeldBannerModel;
import com.talhanation.recruits.items.HeldBannerItem;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.*;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.tileentity.BannerTileEntity;

import java.util.List;


public class RecruitsItemStackTileEntityRenderer extends ItemStackTileEntityRenderer {
    private final RenderInfo renderInfo;
    private final HeldBannerModel heldBannerModel;

    public RecruitsItemStackTileEntityRenderer(RenderInfo renderInfo, HeldBannerModel heldBannerModel) {
        this.renderInfo = renderInfo;
        this.heldBannerModel = new HeldBannerModel();
    }


    public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transformType, MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        Item item = stack.getItem();
        if (item instanceof HeldBannerItem) {
            boolean flag = (stack.getTagElement("BlockEntityTag") != null);
            matrixStack.pushPose();
            matrixStack.scale(1.0F, -1.0F, -1.0F);
            RenderMaterial material = flag ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
            IVertexBuilder vertexBuilder = material.sprite().wrap(ItemRenderer.getFoilBufferDirect(buffer, this.heldBannerModel.renderType(material.atlasLocation()), true, stack.hasFoil()));
            this.heldBannerModel.getHandle().render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            this.heldBannerModel.getPlate().render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            if (flag) {
                List<Pair<BannerPattern, DyeColor>> list = BannerTileEntity.createPatterns(ShieldItem.getColor(stack), BannerTileEntity.getItemPatterns(stack));
                renderBanner(matrixStack, vertexBuilder, buffer, combinedLight, combinedOverlay, this.heldBannerModel.getPlate(), material, false, list);
            }
            matrixStack.popPose();
        }
    }

    private void renderBanner(MatrixStack matrixStack, IVertexBuilder vertexBuilder, IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay, ModelRenderer modelRenderer, RenderMaterial modelMaterial, boolean isBanner, List<Pair<BannerPattern, DyeColor>> bannerList) {
        for (int i = 0; i < 17 && i < bannerList.size(); i++) {
            Pair<BannerPattern, DyeColor> pair = bannerList.get(i);
            float[] afloat = (pair.getSecond()).getTextureDiffuseColors();
            RenderMaterial material = new RenderMaterial(isBanner ? Atlases.BANNER_SHEET : Atlases.SHIELD_SHEET, (pair.getFirst()).location(isBanner));
            modelRenderer.render(matrixStack, material.buffer(renderTypeBuffer, RenderType::entitySolid), combinedLight, combinedOverlay, afloat[0], afloat[1], afloat[2], 1.0F);
        }
    }


}
