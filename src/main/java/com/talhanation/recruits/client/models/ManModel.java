package com.talhanation.recruits.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;

public class ManModel<E extends AbstractInventoryEntity> extends RecruitsBipedModel<E>{
    public ModelPart bipedChest;

    public ManModel() {
        this(0.0F, false);
    }

    public ManModel(float f, boolean armor) {
        super(f, 0.0F, 64, armor ? 32 : 64);
        this.bipedChest = new ModelPart((Model)this, 24, 0);
        this.bipedChest.addBox(-3.0F, 2.0F, -4.0F, 6.0F, 3.0F, 2.0F, f);
        this.bipedChest.setPos(0.0F, 0.0F, 0.0F);
        this.body.addChild(this.bipedChest);
    }

    public void renderToBuffer(PoseStack matStack, VertexConsumer buf, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.bipedChest.visible = this.showChest;
        super.renderToBuffer(matStack, buf, packedLight, packedOverlay, r, g, b, a);
    }

    protected void setupAttackAnimation(E p_230486_1_, float p_230486_2_) {
        if (!(this.attackTime <= 0.0F)) {
            HumanoidArm handside = this.getAttackArm(p_230486_1_);
            ModelPart modelrenderer = this.getArm(handside);
            float f = this.attackTime;
            this.body.yRot = Mth.sin(Mth.sqrt(f) * ((float)Math.PI * 2F)) * 0.2F;
            if (handside == HumanoidArm.LEFT) {
                this.body.yRot *= -1.0F;
            }

            this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
            this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
            this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
            this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
            this.rightArm.yRot += this.body.yRot;
            this.leftArm.yRot += this.body.yRot;
            this.leftArm.xRot += this.body.yRot;
            f = 1.0F - this.attackTime;
            f = f * f;
            f = f * f;
            f = 1.0F - f;
            float f1 = Mth.sin(f * (float)Math.PI);
            float f2 = Mth.sin(this.attackTime * (float)Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
            modelrenderer.xRot = (float)((double)modelrenderer.xRot - ((double)f1 * 1.2D + (double)f2));
            modelrenderer.yRot += this.body.yRot * 2.0F;
            modelrenderer.zRot += Mth.sin(this.attackTime * (float)Math.PI) * -0.4F;
        }
    }
}

