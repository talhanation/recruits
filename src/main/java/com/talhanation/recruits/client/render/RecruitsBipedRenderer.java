package com.talhanation.recruits.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;


public class RecruitsBipedRenderer <E extends CreatureEntity, M extends BipedModel<E>> extends BipedRenderer<E, M>{

    public static final float PLAYER_SCALE = 0.9375F;

    protected static final float BIPED_SHADOW_SIZE = 0.5F;

    public RecruitsBipedRenderer(EntityRendererManager manager, M model, M leggingsModel, M mainArmorModel, float shadowSize) {
        super(manager, model, shadowSize);
        addLayer(new BipedArmorLayer(this, leggingsModel, mainArmorModel));
    }

    public void render(E entity, float yaw, float partialTicks, MatrixStack matStack, IRenderTypeBuffer buf, int packedLight) {
        setArmPoses(entity);
        super.render(entity, yaw, partialTicks, matStack, buf, packedLight);
    }

    private void setArmPoses(E entity) {
        BipedModel.ArmPose mainArmPose = getArmPose((LivingEntity)entity, Hand.MAIN_HAND);
        BipedModel.ArmPose offArmPose = getArmPose((LivingEntity)entity, Hand.OFF_HAND);
        if (mainArmPose.isTwoHanded())
            offArmPose = entity.getOffhandItem().isEmpty() ? BipedModel.ArmPose.EMPTY : BipedModel.ArmPose.ITEM;
        if (entity.getMainArm() == HandSide.RIGHT) {
            (this.model).rightArmPose = mainArmPose;
            (this.model).leftArmPose = offArmPose;
        } else {
            (this.model).rightArmPose = offArmPose;
            (this.model).leftArmPose = mainArmPose;
        }
    }

    private static BipedModel.ArmPose getArmPose(LivingEntity entity, Hand hand) {
        ItemStack heldItem = entity.getItemInHand(hand);
        if (heldItem.isEmpty())
            return BipedModel.ArmPose.EMPTY;
        if (entity.getUsedItemHand() == hand && entity.getTicksUsingItem() > 0) {
            UseAction useaction = heldItem.getUseAnimation();
            if (useaction == UseAction.BLOCK)
                return BipedModel.ArmPose.BLOCK;
            if (useaction == UseAction.BOW)
                return BipedModel.ArmPose.BOW_AND_ARROW;
            if (useaction == UseAction.SPEAR)
                return BipedModel.ArmPose.THROW_SPEAR;
            if (useaction == UseAction.CROSSBOW && hand == entity.getUsedItemHand())
                return BipedModel.ArmPose.CROSSBOW_CHARGE;
        } else if (!entity.isUsingItem() && heldItem.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(heldItem)) {
            return BipedModel.ArmPose.CROSSBOW_HOLD;
        }
        return BipedModel.ArmPose.ITEM;
    }

}
