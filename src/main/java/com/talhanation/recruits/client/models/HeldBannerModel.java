package com.talhanation.recruits.client.models;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeldBannerModel extends Model {
	private final ModelPart handle;
	private final ModelPart plate;

	public HeldBannerModel() {
		super(RenderType::entitySolid);
		texWidth = 16;
		texHeight = 16;

		handle = new ModelPart(this, 0, 0);
		handle.setPos(0.0F, 24.0F, 0.0F);
		handle.texOffs(0, 0).addBox(-1.0F, -30.0F, 0.0F, 1.0F, 15.0F, 1.0F, 0.0F, false);
		handle.texOffs(0, 0).addBox(-1.0F, -15.0F, 0.0F, 1.0F, 15.0F, 1.0F, 0.0F, false);
		this.plate = new ModelPart(this, 0, 0);
		this.plate.addBox(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F, 0.0F);
	}

	@Override
	public void renderToBuffer(PoseStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		handle.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	public ModelPart getPlate() {
		return this.plate;
	}

	public ModelPart getHandle() {
		return this.handle;
	}
}