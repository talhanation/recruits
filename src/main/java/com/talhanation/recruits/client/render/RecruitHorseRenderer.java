package com.talhanation.recruits.client.render;

import com.talhanation.recruits.client.models.RecruitHorseModel;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.entity.passive.horse.CoatColors;
import net.minecraft.util.ResourceLocation;

public class RecruitHorseRenderer extends MobRenderer<RecruitHorseEntity, RecruitHorseModel> {
    private static final ResourceLocation[] TEXTURE = new ResourceLocation[]{
            new ResourceLocation("textures/entity/horse/horse_white.png"),
            new ResourceLocation("textures/entity/horse/horse_white.png"),
            new ResourceLocation("textures/entity/horse/horse_creamy.png"),
            new ResourceLocation("textures/entity/horse/horse_chestnut.png"),
            new ResourceLocation("textures/entity/horse/horse_brown.png"),
            new ResourceLocation("textures/entity/horse/horse_black.png"),
            new ResourceLocation("textures/entity/horse/horse_gray.png"),
            new ResourceLocation("textures/entity/horse/horse_darkbrown.png")};


    public RecruitHorseRenderer (EntityRendererManager p_i47205_1_) {
        super(p_i47205_1_, new RecruitHorseModel(0F), 1);
    }

    public ResourceLocation getTextureLocation(RecruitHorseEntity horse) {
        return TEXTURE[horse.getTypeVariant()];
    }

}
