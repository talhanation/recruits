package com.talhanation.recruits.client.models;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

public class RecruitHorseModel extends EntityModel<RecruitHorseEntity> {
    protected final ModelRenderer body;
    protected final ModelRenderer headParts;
    private final ModelRenderer leg1;
    private final ModelRenderer leg2;
    private final ModelRenderer leg3;
    private final ModelRenderer leg4;
    private final ModelRenderer babyLeg1;
    private final ModelRenderer babyLeg2;
    private final ModelRenderer babyLeg3;
    private final ModelRenderer babyLeg4;
    private final ModelRenderer tail;
    private final ModelRenderer[] saddleParts;
    private final ModelRenderer[] ridingParts;

    public RecruitHorseModel() {
        super();
        float p_i51065_1_ = 0;
        this.texWidth = 64;
        this.texHeight = 64;
        this.body = new ModelRenderer(this, 0, 32);
        this.body.addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 22.0F, 0.05F);
        this.body.setPos(0.0F, 11.0F, 5.0F);
        this.headParts = new ModelRenderer(this, 0, 35);
        this.headParts.addBox(-2.05F, -6.0F, -2.0F, 4.0F, 12.0F, 7.0F);
        this.headParts.xRot = 0.5235988F;
        ModelRenderer lvt_2_1_ = new ModelRenderer(this, 0, 13);
        lvt_2_1_.addBox(-3.0F, -11.0F, -2.0F, 6.0F, 5.0F, 7.0F, p_i51065_1_);
        ModelRenderer lvt_3_1_ = new ModelRenderer(this, 56, 36);
        lvt_3_1_.addBox(-1.0F, -11.0F, 5.01F, 2.0F, 16.0F, 2.0F, p_i51065_1_);
        ModelRenderer lvt_4_1_ = new ModelRenderer(this, 0, 25);
        lvt_4_1_.addBox(-2.0F, -11.0F, -7.0F, 4.0F, 5.0F, 5.0F, p_i51065_1_);
        this.headParts.addChild(lvt_2_1_);
        this.headParts.addChild(lvt_3_1_);
        this.headParts.addChild(lvt_4_1_);
        this.addEarModels(this.headParts);
        this.leg1 = new ModelRenderer(this, 48, 21);
        this.leg1.mirror = true;
        this.leg1.addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, p_i51065_1_);
        this.leg1.setPos(4.0F, 14.0F, 7.0F);
        this.leg2 = new ModelRenderer(this, 48, 21);
        this.leg2.addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, p_i51065_1_);
        this.leg2.setPos(-4.0F, 14.0F, 7.0F);
        this.leg3 = new ModelRenderer(this, 48, 21);
        this.leg3.mirror = true;
        this.leg3.addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, p_i51065_1_);
        this.leg3.setPos(4.0F, 6.0F, -12.0F);
        this.leg4 = new ModelRenderer(this, 48, 21);
        this.leg4.addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, p_i51065_1_);
        this.leg4.setPos(-4.0F, 6.0F, -12.0F);
        float lvt_5_1_ = 5.5F;
        this.babyLeg1 = new ModelRenderer(this, 48, 21);
        this.babyLeg1.mirror = true;
        this.babyLeg1.addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, p_i51065_1_, p_i51065_1_ + 5.5F, p_i51065_1_);
        this.babyLeg1.setPos(4.0F, 14.0F, 7.0F);
        this.babyLeg2 = new ModelRenderer(this, 48, 21);
        this.babyLeg2.addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, p_i51065_1_, p_i51065_1_ + 5.5F, p_i51065_1_);
        this.babyLeg2.setPos(-4.0F, 14.0F, 7.0F);
        this.babyLeg3 = new ModelRenderer(this, 48, 21);
        this.babyLeg3.mirror = true;
        this.babyLeg3.addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, p_i51065_1_, p_i51065_1_ + 5.5F, p_i51065_1_);
        this.babyLeg3.setPos(4.0F, 6.0F, -12.0F);
        this.babyLeg4 = new ModelRenderer(this, 48, 21);
        this.babyLeg4.addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, p_i51065_1_, p_i51065_1_ + 5.5F, p_i51065_1_);
        this.babyLeg4.setPos(-4.0F, 6.0F, -12.0F);
        this.tail = new ModelRenderer(this, 42, 36);
        this.tail.addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 4.0F, p_i51065_1_);
        this.tail.setPos(0.0F, -5.0F, 2.0F);
        this.tail.xRot = 0.5235988F;
        this.body.addChild(this.tail);
        ModelRenderer lvt_6_1_ = new ModelRenderer(this, 26, 0);
        lvt_6_1_.addBox(-5.0F, -8.0F, -9.0F, 10.0F, 9.0F, 9.0F, 0.5F);
        this.body.addChild(lvt_6_1_);
        ModelRenderer lvt_7_1_ = new ModelRenderer(this, 29, 5);
        lvt_7_1_.addBox(2.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F, p_i51065_1_);
        this.headParts.addChild(lvt_7_1_);
        ModelRenderer lvt_8_1_ = new ModelRenderer(this, 29, 5);
        lvt_8_1_.addBox(-3.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F, p_i51065_1_);
        this.headParts.addChild(lvt_8_1_);
        ModelRenderer lvt_9_1_ = new ModelRenderer(this, 32, 2);
        lvt_9_1_.addBox(3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F, p_i51065_1_);
        lvt_9_1_.xRot = -0.5235988F;
        this.headParts.addChild(lvt_9_1_);
        ModelRenderer lvt_10_1_ = new ModelRenderer(this, 32, 2);
        lvt_10_1_.addBox(-3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F, p_i51065_1_);
        lvt_10_1_.xRot = -0.5235988F;
        this.headParts.addChild(lvt_10_1_);
        ModelRenderer lvt_11_1_ = new ModelRenderer(this, 1, 1);
        lvt_11_1_.addBox(-3.0F, -11.0F, -1.9F, 6.0F, 5.0F, 6.0F, 0.2F);
        this.headParts.addChild(lvt_11_1_);
        ModelRenderer lvt_12_1_ = new ModelRenderer(this, 19, 0);
        lvt_12_1_.addBox(-2.0F, -11.0F, -4.0F, 4.0F, 5.0F, 2.0F, 0.2F);
        this.headParts.addChild(lvt_12_1_);
        this.saddleParts = new ModelRenderer[]{lvt_6_1_, lvt_7_1_, lvt_8_1_, lvt_11_1_, lvt_12_1_};
        this.ridingParts = new ModelRenderer[]{lvt_9_1_, lvt_10_1_};
    }

    protected void addEarModels(ModelRenderer p_199047_1_) {
        ModelRenderer lvt_2_1_ = new ModelRenderer(this, 19, 16);
        lvt_2_1_.addBox(0.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, -0.001F);
        ModelRenderer lvt_3_1_ = new ModelRenderer(this, 19, 16);
        lvt_3_1_.addBox(-2.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, -0.001F);
        p_199047_1_.addChild(lvt_2_1_);
        p_199047_1_.addChild(lvt_3_1_);
    }

    public void setupAnim(RecruitHorseEntity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {
        boolean lvt_7_1_ = true;
        boolean lvt_8_1_ = p_225597_1_.isVehicle();
        ModelRenderer[] var9 = this.saddleParts;
        int var10 = var9.length;

        int var11;
        ModelRenderer lvt_12_2_;
        for(var11 = 0; var11 < var10; ++var11) {
            lvt_12_2_ = var9[var11];
            lvt_12_2_.visible = lvt_7_1_;
        }

        var9 = this.ridingParts;
        var10 = var9.length;

        for(var11 = 0; var11 < var10; ++var11) {
            lvt_12_2_ = var9[var11];
            lvt_12_2_.visible = lvt_8_1_;
        }

        this.body.y = 11.0F;
    }

    public Iterable<ModelRenderer> headParts() {
        return ImmutableList.of(this.headParts);
    }

    protected Iterable<ModelRenderer> bodyParts() {
        return ImmutableList.of(this.body, this.leg1, this.leg2, this.leg3, this.leg4, this.babyLeg1, this.babyLeg2, this.babyLeg3, this.babyLeg4);
    }

    public void prepareMobModel(RecruitHorseEntity mount, float p_212843_2_, float p_212843_3_, float p_212843_4_) {
        super.prepareMobModel(mount, p_212843_2_, p_212843_3_, p_212843_4_);
        float lvt_5_1_ = MathHelper.rotlerp(mount.yBodyRotO, mount.yBodyRot, p_212843_4_);
        float lvt_6_1_ = MathHelper.rotlerp(mount.yHeadRotO, mount.yHeadRot, p_212843_4_);
        float lvt_7_1_ = MathHelper.lerp(p_212843_4_, mount.xRotO, mount.xRot);
        float lvt_8_1_ = lvt_6_1_ - lvt_5_1_;
        float lvt_9_1_ = lvt_7_1_ * 0.017453292F;
        if (lvt_8_1_ > 20.0F) {
            lvt_8_1_ = 20.0F;
        }

        if (lvt_8_1_ < -20.0F) {
            lvt_8_1_ = -20.0F;
        }

        if (p_212843_3_ > 0.2F) {
            lvt_9_1_ += MathHelper.cos(p_212843_2_ * 0.4F) * 0.15F * p_212843_3_;
        }

        float lvt_10_1_ = 0; // p_212843_1_.getEatAnim(p_212843_4_);
        float lvt_11_1_ = 0; // p_212843_1_.getStandAnim(p_212843_4_);
        float lvt_12_1_ = 1.0F - lvt_11_1_;
        float lvt_13_1_ = 0; // p_212843_1_.getMouthAnim(p_212843_4_);
        boolean lvt_14_1_ = false; // p_212843_1_.tailCounter != 0;
        float lvt_15_1_ = (float)mount.tickCount + p_212843_4_;
        this.headParts.y = 4.0F;
        this.headParts.z = -12.0F;
        this.body.xRot = 0.0F;
        this.headParts.xRot = 0.5235988F + lvt_9_1_;
        this.headParts.yRot = lvt_8_1_ * 0.017453292F;
        float lvt_16_1_ = mount.isInWater() ? 0.2F : 1.0F;
        float lvt_17_1_ = MathHelper.cos(lvt_16_1_ * p_212843_2_ * 0.6662F + 3.1415927F);
        float lvt_18_1_ = lvt_17_1_ * 0.8F * p_212843_3_;
        float lvt_19_1_ = (1.0F - Math.max(lvt_11_1_, lvt_10_1_)) * (0.5235988F + lvt_9_1_ + lvt_13_1_ * MathHelper.sin(lvt_15_1_) * 0.05F);
        this.headParts.xRot = lvt_11_1_ * (0.2617994F + lvt_9_1_) + lvt_10_1_ * (2.1816616F + MathHelper.sin(lvt_15_1_) * 0.05F) + lvt_19_1_;
        this.headParts.yRot = lvt_11_1_ * lvt_8_1_ * 0.017453292F + (1.0F - Math.max(lvt_11_1_, lvt_10_1_)) * this.headParts.yRot;
        this.headParts.y = lvt_11_1_ * -4.0F + lvt_10_1_ * 11.0F + (1.0F - Math.max(lvt_11_1_, lvt_10_1_)) * this.headParts.y;
        this.headParts.z = lvt_11_1_ * -4.0F + lvt_10_1_ * -12.0F + (1.0F - Math.max(lvt_11_1_, lvt_10_1_)) * this.headParts.z;
        this.body.xRot = lvt_11_1_ * -0.7853982F + lvt_12_1_ * this.body.xRot;
        float lvt_20_1_ = 0.2617994F * lvt_11_1_;
        float lvt_21_1_ = MathHelper.cos(lvt_15_1_ * 0.6F + 3.1415927F);
        this.leg3.y = 2.0F * lvt_11_1_ + 14.0F * lvt_12_1_;
        this.leg3.z = -6.0F * lvt_11_1_ - 10.0F * lvt_12_1_;
        this.leg4.y = this.leg3.y;
        this.leg4.z = this.leg3.z;
        float lvt_22_1_ = (-1.0471976F + lvt_21_1_) * lvt_11_1_ + lvt_18_1_ * lvt_12_1_;
        float lvt_23_1_ = (-1.0471976F - lvt_21_1_) * lvt_11_1_ - lvt_18_1_ * lvt_12_1_;
        this.leg1.xRot = lvt_20_1_ - lvt_17_1_ * 0.5F * p_212843_3_ * lvt_12_1_;
        this.leg2.xRot = lvt_20_1_ + lvt_17_1_ * 0.5F * p_212843_3_ * lvt_12_1_;
        this.leg3.xRot = lvt_22_1_;
        this.leg4.xRot = lvt_23_1_;
        this.tail.xRot = 0.5235988F + p_212843_3_ * 0.75F;
        this.tail.y = -5.0F + p_212843_3_;
        this.tail.z = 2.0F + p_212843_3_ * 2.0F;
        if (lvt_14_1_) {
            this.tail.yRot = MathHelper.cos(lvt_15_1_ * 0.7F);
        } else {
            this.tail.yRot = 0.0F;
        }

        this.babyLeg1.y = this.leg1.y;
        this.babyLeg1.z = this.leg1.z;
        this.babyLeg1.xRot = this.leg1.xRot;
        this.babyLeg2.y = this.leg2.y;
        this.babyLeg2.z = this.leg2.z;
        this.babyLeg2.xRot = this.leg2.xRot;
        this.babyLeg3.y = this.leg3.y;
        this.babyLeg3.z = this.leg3.z;
        this.babyLeg3.xRot = this.leg3.xRot;
        this.babyLeg4.y = this.leg4.y;
        this.babyLeg4.z = this.leg4.z;
        this.babyLeg4.xRot = this.leg4.xRot;
        boolean lvt_24_1_ = false; // p_212843_1_.isBaby();
        this.leg1.visible = !lvt_24_1_;
        this.leg2.visible = !lvt_24_1_;
        this.leg3.visible = !lvt_24_1_;
        this.leg4.visible = !lvt_24_1_;
        this.babyLeg1.visible = lvt_24_1_;
        this.babyLeg2.visible = lvt_24_1_;
        this.babyLeg3.visible = lvt_24_1_;
        this.babyLeg4.visible = lvt_24_1_;
        this.body.y = lvt_24_1_ ? 10.8F : 0.0F;
    }

    @Override
    public void renderToBuffer(MatrixStack p_225598_1_, IVertexBuilder p_225598_2_, int p_225598_3_, int p_225598_4_, float p_225598_5_, float p_225598_6_, float p_225598_7_, float p_225598_8_) {
        this.headParts().forEach((p_228228_8_) -> {
            p_228228_8_.render(p_225598_1_, p_225598_2_, p_225598_3_, p_225598_4_, p_225598_5_, p_225598_6_, p_225598_7_, p_225598_8_);
        });
        this.bodyParts().forEach((p_228227_8_) -> {
            p_228227_8_.render(p_225598_1_, p_225598_2_, p_225598_3_, p_225598_4_, p_225598_5_, p_225598_6_, p_225598_7_, p_225598_8_);
        });
    }
}