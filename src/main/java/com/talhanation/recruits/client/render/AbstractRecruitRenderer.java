package com.talhanation.recruits.client.render;

import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.client.models.RecruitModel;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

public abstract class AbstractRecruitRenderer<T extends AbstractInventoryEntity, M extends RecruitModel<T>> extends HumanoidMobRenderer<T, M> {
    public AbstractRecruitRenderer(EntityRendererProvider.Context mgr) {
        super(mgr, (M) new RecruitModel (mgr.bakeLayer(ClientEvent.RECRUIT)), 0.5F);
        //this.addLayer(new HumanoidArmorLayer<>(this, new RecruitModel(mgr.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new RecruitModel(mgr.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
        //this.addLayer(new ArrowLayer<>(mgr, this));
        //this.addLayer(new BeeStingerLayer<>(this));
        //this.addLayer(new CustomHeadLayer<>(this, mgr.getModelSet()));
        //this.addLayer(new ItemInHandLayer<>(this));
    }

    private static HumanoidModel.ArmPose getArmPose(AbstractInventoryEntity recruit, InteractionHand hand) {
        ItemStack itemstack = recruit.getItemInHand(hand);
        if (itemstack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else {
            if (recruit.getUsedItemHand() == hand && recruit.getUseItemRemainingTicks() > 0) {
                UseAnim useanim = itemstack.getUseAnimation();
                if (useanim == UseAnim.BLOCK)
                    return HumanoidModel.ArmPose.BLOCK;

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
                
                if (recruit.getUsedItemHand() == hand && recruit.getItemInHand(hand) == Items.SHIELD.getDefaultInstance()){
                    return HumanoidModel.ArmPose.ITEM;
                }
            } else if (!recruit.swinging && itemstack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemstack)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }

            return HumanoidModel.ArmPose.ITEM;
        }
    }
}
