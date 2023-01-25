package com.talhanation.recruits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.client.models.RecruitVillagerModel;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

public abstract class AbstractRecruitVillagerRenderer extends HumanoidMobRenderer<AbstractInventoryEntity, HumanoidModel<AbstractInventoryEntity>> {

    public AbstractRecruitVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new RecruitVillagerModel(context.bakeLayer(ClientEvent.RECRUIT)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ClientEvent.RECRUIT_OUTER_ARMOR))));
        //layer
    }

    @Override
    public void render(AbstractInventoryEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        this.setModelVisibilities(entityIn);
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }
    //PlayerRenderer
    private void setModelVisibilities(AbstractInventoryEntity recruitEntity) {
        HumanoidModel<AbstractInventoryEntity> model = this.getModel();
        model.setAllVisible(true);
        model.crouching = recruitEntity.isCrouching();

        HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(recruitEntity, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(recruitEntity, InteractionHand.OFF_HAND);
        if (humanoidmodel$armpose.isTwoHanded()) {
            humanoidmodel$armpose1 = recruitEntity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }

        if (recruitEntity.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = humanoidmodel$armpose;
            model.leftArmPose = humanoidmodel$armpose1;
        } else {
            model.rightArmPose = humanoidmodel$armpose1;
            model.leftArmPose = humanoidmodel$armpose;
        }
    }

    @Override
    protected void scale(AbstractInventoryEntity entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9375F, 0.9375F, 0.9375F);
    }

    private static HumanoidModel.ArmPose getArmPose(AbstractInventoryEntity recruit, InteractionHand hand) {
        ItemStack itemstack = recruit.getItemInHand(hand);
        if (itemstack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else {
            if (recruit.getUsedItemHand() == hand && recruit.getUseItemRemainingTicks() > 0) {
                UseAnim useanim = itemstack.getUseAnimation();
                if (useanim == UseAnim.BLOCK) {
                    return HumanoidModel.ArmPose.BLOCK;
                }

                if (useanim == UseAnim.BOW) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }

                if (useanim == UseAnim.SPEAR) {
                    return HumanoidModel.ArmPose.THROW_SPEAR;
                }

                if (useanim == UseAnim.CROSSBOW && hand == recruit.getUsedItemHand()) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useanim == UseAnim.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }
            } else if (!recruit.swinging && itemstack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemstack)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            return HumanoidModel.ArmPose.ITEM;
        }
    }

    public abstract ResourceLocation getTextureLocation(AbstractInventoryEntity p_110775_1_);
}
