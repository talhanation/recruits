package com.talhanation.recruits.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.client.render.layer.RecruitHumanBiomeLayer;
import com.talhanation.recruits.client.render.layer.RecruitHumanCompanionLayer;
import com.talhanation.recruits.client.render.layer.RecruitHumanTeamColorLayer;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;
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

public class RecruitHumanRenderer extends MobRenderer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_0.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_4.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_5.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_6.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_7.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_8.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_9.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_10.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_11.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_12.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_13.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_14.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_15.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_16.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_17.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_18.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_19.png")
    };

    @Override
    public ResourceLocation getTextureLocation(AbstractRecruitEntity recruit) {
        return TEXTURE[recruit.getVariant()];
    }
    public RecruitHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr, new HumanoidModel<>((mgr.bakeLayer(ModelLayers.PLAYER))), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel(mgr.bakeLayer(ClientEvent.RECRUIT_INNER_ARMOR)), new HumanoidModel(mgr.bakeLayer(ClientEvent.RECRUIT_OUTER_ARMOR))));
        this.addLayer(new RecruitHumanTeamColorLayer(this));
        this.addLayer(new RecruitHumanBiomeLayer(this));
        this.addLayer(new RecruitHumanCompanionLayer(this));
        //this.addLayer(new ArrowLayer<>(mgr, this));
        this.addLayer(new ItemInHandLayer<>(this, mgr.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, mgr.getModelSet(), mgr.getItemInHandRenderer()));

    }


    public void render(AbstractRecruitEntity recruit, float p_117789_, float p_117790_, PoseStack p_117791_, MultiBufferSource p_117792_, int p_117793_) {
        this.setModelProperties(recruit);
        super.render(recruit, p_117789_, p_117790_, p_117791_, p_117792_, p_117793_);
    }

    private void setModelProperties(AbstractRecruitEntity recruit) {
        HumanoidModel<AbstractRecruitEntity> model = this.getModel();

        model.setAllVisible(true);
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

    private static HumanoidModel.ArmPose getArmPose(AbstractRecruitEntity recruit, InteractionHand hand) {
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

            HumanoidModel.ArmPose forgeArmPose = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(itemstack).getArmPose(recruit, hand, itemstack);
            if (forgeArmPose != null) return forgeArmPose;

            return HumanoidModel.ArmPose.ITEM;
        }
    }

}