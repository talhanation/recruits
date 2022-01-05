package com.talhanation.recruits.client.models;


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeldBannerModel extends Model {
	private final ModelRenderer handle;
	private final ModelRenderer plate;

	public HeldBannerModel() {
		super(RenderType::entitySolid);
		texWidth = 16;
		texHeight = 16;

		handle = new ModelRenderer(this, 0, 0);
		handle.setPos(0.0F, 24.0F, 0.0F);
		handle.texOffs(0, 0).addBox(-1.0F, -30.0F, 0.0F, 1.0F, 15.0F, 1.0F, 0.0F, false);
		handle.texOffs(0, 0).addBox(-1.0F, -15.0F, 0.0F, 1.0F, 15.0F, 1.0F, 0.0F, false);
		this.plate = new ModelRenderer(this, 0, 0);
		this.plate.addBox(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F, 0.0F);
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		handle.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	public ModelRenderer getPlate() {
		return this.plate;
	}

	public ModelRenderer getHandle() {
		return this.handle;
	}
}