package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.RecruitBaseEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;


public class RenderRecruitEntity extends MobRenderer<RecruitBaseEntity, PlayerModel<RecruitBaseEntity>> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/entity/example/example.png");


    public RenderRecruitEntity(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(RecruitBaseEntity p_110775_1_) {
        return TEXTURE;
    }


}
