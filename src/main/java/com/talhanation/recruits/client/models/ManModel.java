package com.talhanation.recruits.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;

public class ManModel<E extends AbstractInventoryEntity> extends RecruitsBipedModel<E>{
    public ModelRenderer bipedChest;

    public ManModel() {
        this(0.0F, false);
    }

    public ManModel(float f, boolean armor) {
        super(f, 0.0F, 64, armor ? 32 : 64);
        this.bipedChest = new ModelRenderer((Model)this, 24, 0);
        this.bipedChest.addBox(-3.0F, 2.0F, -4.0F, 6.0F, 3.0F, 2.0F, f);
        this.bipedChest.setPos(0.0F, 0.0F, 0.0F);
        this.body.addChild(this.bipedChest);
    }

    public void renderToBuffer(MatrixStack matStack, IVertexBuilder buf, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.bipedChest.visible = this.showChest;
        super.renderToBuffer(matStack, buf, packedLight, packedOverlay, r, g, b, a);
    }

    protected void setupAttackAnimation(E p_230486_1_, float p_230486_2_) {
        if (!(this.attackTime <= 0.0F)) {
            HandSide handside = this.getAttackArm(p_230486_1_);
            ModelRenderer modelrenderer = this.getArm(handside);
            float f = this.attackTime;
            this.body.yRot = MathHelper.sin(MathHelper.sqrt(f) * ((float)Math.PI * 2F)) * 0.2F;
            if (handside == HandSide.LEFT) {
                this.body.yRot *= -1.0F;
            }

            this.rightArm.z = MathHelper.sin(this.body.yRot) * 5.0F;
            this.rightArm.x = -MathHelper.cos(this.body.yRot) * 5.0F;
            this.leftArm.z = -MathHelper.sin(this.body.yRot) * 5.0F;
            this.leftArm.x = MathHelper.cos(this.body.yRot) * 5.0F;
            this.rightArm.yRot += this.body.yRot;
            this.leftArm.yRot += this.body.yRot;
            this.leftArm.xRot += this.body.yRot;
            f = 1.0F - this.attackTime;
            f = f * f;
            f = f * f;
            f = 1.0F - f;
            float f1 = MathHelper.sin(f * (float)Math.PI);
            float f2 = MathHelper.sin(this.attackTime * (float)Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
            modelrenderer.xRot = (float)((double)modelrenderer.xRot - ((double)f1 * 1.2D + (double)f2));
            modelrenderer.yRot += this.body.yRot * 2.0F;
            modelrenderer.zRot += MathHelper.sin(this.attackTime * (float)Math.PI) * -0.4F;
        }
    }
}

