package com.talhanation.recruits.client.render.layer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VillagerRecruitCustomHeadLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final ItemInHandRenderer itemInHandRenderer;

    public VillagerRecruitCustomHeadLayer(RenderLayerParent<T, M> p_234829_, EntityModelSet p_234830_, ItemInHandRenderer p_234831_) {
        this(p_234829_, p_234830_, 1.0F, 1.0F, 1.0F, p_234831_);
    }

    public VillagerRecruitCustomHeadLayer(RenderLayerParent<T, M> p_234822_, EntityModelSet p_234823_, float p_234824_, float p_234825_, float p_234826_, ItemInHandRenderer p_234827_) {
        super(p_234822_);
        this.scaleX = p_234824_;
        this.scaleY = p_234825_;
        this.scaleZ = p_234826_;
        this.skullModels = SkullBlockRenderer.createSkullRenderers(p_234823_);
        this.itemInHandRenderer = p_234827_;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int p_116733_, T entity, float p_116735_, float p_116736_, float p_116737_, float p_116738_, float p_116739_, float p_116740_) {
        ItemStack itemstack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!itemstack.isEmpty()) {
            Item item = itemstack.getItem();
            poseStack.pushPose();
            poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
            boolean flag = true;
            if (entity.isBaby() && !(entity instanceof Villager)) {
                float f = 2.0F;
                float f1 = 1.4F;
                poseStack.translate(0.0D, 0.03125D, 0.0D);
                poseStack.scale(0.7F, 0.7F, 0.7F);
                poseStack.translate(0.0D, 1.0D, 0.0D);
            }

            this.getParentModel().getHead().translateAndRotate(poseStack);
            if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof AbstractSkullBlock) {
                float f2 = 1.1875F;
                poseStack.scale(f2, -f2, -f2);
                poseStack.translate(0.0D, 0.0625D, 0.0D);

                GameProfile gameprofile = null;
                if (itemstack.hasTag()) {
                    CompoundTag compoundtag = itemstack.getTag();
                    if (compoundtag.contains("SkullOwner", 10)) {
                        gameprofile = NbtUtils.readGameProfile(compoundtag.getCompound("SkullOwner"));
                    }
                }

                poseStack.translate(-0.5D, 0.0D, -0.5D);
                SkullBlock.Type skullblock$type = ((AbstractSkullBlock) ((BlockItem) item).getBlock()).getType();
                SkullModelBase skullmodelbase = this.skullModels.get(skullblock$type);
                RenderType rendertype = SkullBlockRenderer.getRenderType(skullblock$type, gameprofile);
                SkullBlockRenderer.renderSkull((Direction) null, 180.0F, p_116735_, poseStack, bufferSource, p_116733_, skullmodelbase, rendertype);
            } else if (!(item instanceof ArmorItem) || ((ArmorItem) item).getEquipmentSlot() != EquipmentSlot.HEAD) {
                translateToHead(poseStack, flag);
                this.itemInHandRenderer.renderItem(entity, itemstack, ItemDisplayContext.HEAD, false, poseStack, bufferSource, p_116733_);
            }
            poseStack.popPose();

        }
    }

    public static void translateToHead(PoseStack p_174484_, boolean p_174485_) {
        float f = 0.625F;
        p_174484_.translate(0.0D, -0.25D, 0.0D);
        p_174484_.mulPose(Axis.YP.rotationDegrees(180.0F));
        p_174484_.scale(f, -f, -f);
        p_174484_.translate(0.0D, 0.1875D, 0.0D);
    }
}
