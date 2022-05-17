package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RecruitRenderer<Type extends AbstractRecruitEntity> extends MobRenderer<Type, PlayerModel<Type>> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public RecruitRenderer(EntityRendererProvider.Context mgr) {
        super(mgr, new PlayerModel<>((mgr.bakeLayer(ModelLayers.PLAYER)), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractRecruitEntity recruit) {
        return TEXTURE[0];
    }
}
