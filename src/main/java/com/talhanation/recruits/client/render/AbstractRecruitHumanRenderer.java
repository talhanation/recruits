package com.talhanation.recruits.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

public abstract class AbstractRecruitHumanRenderer<Type extends AbstractInventoryEntity> extends MobRenderer<Type, PlayerModel<Type>> {

    public AbstractRecruitHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr, new PlayerModel<>((mgr.bakeLayer(ModelLayers.PLAYER)), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel(mgr.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(mgr.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
        this.addLayer(new ArrowLayer<>(mgr, this));
        this.addLayer(new BeeStingerLayer<>(this));
        this.addLayer(new ItemInHandLayer<>(this, mgr.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, mgr.getModelSet(), mgr.getItemInHandRenderer()));
    }

    public void render(AbstractInventoryEntity recruit, float p_117789_, float p_117790_, PoseStack p_117791_, MultiBufferSource p_117792_, int p_117793_) {
        this.setModelProperties(recruit);
        super.render((Type) recruit, p_117789_, p_117790_, p_117791_, p_117792_, p_117793_);
    }

    private void setModelProperties(AbstractInventoryEntity recruit) {
        PlayerModel<AbstractInventoryEntity> model = (PlayerModel<AbstractInventoryEntity>) this.getModel();

        model.setAllVisible(true);
        model.hat.visible = true;
        model.jacket.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.crouching = recruit.isCrouching();
        HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(recruit, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(recruit, InteractionHand.OFF_HAND);
        if (humanoidmodel$armpose.isTwoHanded()) {
            humanoidmodel$armpose1 = recruit.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }
        if (recruit.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = humanoidmodel$armpose;
            model.leftArmPose = humanoidmodel$armpose1;
        } else {
            model.rightArmPose = humanoidmodel$armpose1;
            model.leftArmPose = humanoidmodel$armpose;
        }
    }

    private static HumanoidModel.ArmPose getArmPose(AbstractInventoryEntity recruit, InteractionHand hand) {
        ItemStack itemstack = recruit.getItemInHand(hand);
        boolean isMusket = IWeapon.isMusketModWeapon(itemstack) && (recruit instanceof CrossBowmanEntity crossBowman)  && crossBowman.isAggressive();
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

                if (useanim == UseAnim.CROSSBOW && hand == recruit.getUsedItemHand() || isMusket) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useanim == UseAnim.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }
            } else if (!recruit.swinging && itemstack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemstack) || isMusket) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            return HumanoidModel.ArmPose.ITEM;
        }
    }
}