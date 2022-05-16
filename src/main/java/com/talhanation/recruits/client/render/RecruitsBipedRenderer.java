package com.talhanation.recruits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;


public class RecruitsBipedRenderer <E extends PathfinderMob, M extends HumanoidModel<E>> extends HumanoidMobRenderer<E, M>{

    public static final float PLAYER_SCALE = 0.9375F;

    protected static final float BIPED_SHADOW_SIZE = 0.5F;

    public RecruitsBipedRenderer(EntityRenderDispatcher manager, M model, M leggingsModel, M mainArmorModel, float shadowSize) {
        super(manager, model, shadowSize);
        addLayer(new HumanoidArmorLayer(this, leggingsModel, mainArmorModel));
    }

    public void render(E entity, float yaw, float partialTicks, PoseStack matStack, MultiBufferSource buf, int packedLight) {
        setArmPoses(entity);
        super.render(entity, yaw, partialTicks, matStack, buf, packedLight);
    }

    private void setArmPoses(E entity) {
        HumanoidModel.ArmPose mainArmPose = getArmPose((LivingEntity)entity, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose offArmPose = getArmPose((LivingEntity)entity, InteractionHand.OFF_HAND);
        if (mainArmPose.isTwoHanded())
            offArmPose = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            (this.model).rightArmPose = mainArmPose;
            (this.model).leftArmPose = offArmPose;
        } else {
            (this.model).rightArmPose = offArmPose;
            (this.model).leftArmPose = mainArmPose;
        }
    }

    private static HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand) {
        ItemStack heldItem = entity.getItemInHand(hand);
        if (heldItem.isEmpty())
            return HumanoidModel.ArmPose.EMPTY;
        if (entity.getUsedItemHand() == hand && entity.getTicksUsingItem() > 0) {
            UseAnim useaction = heldItem.getUseAnimation();
            if (useaction == UseAnim.BLOCK)
                return HumanoidModel.ArmPose.BLOCK;
            if (useaction == UseAnim.BOW)
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            if (useaction == UseAnim.SPEAR)
                return HumanoidModel.ArmPose.THROW_SPEAR;
            if (useaction == UseAnim.CROSSBOW && hand == entity.getUsedItemHand())
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        } else if (!entity.isUsingItem() && heldItem.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(heldItem)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

}
