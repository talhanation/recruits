package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RecruitRenderer extends AbstractRecruitRenderer<RecruitEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    @Override
    public ResourceLocation getTextureLocation(RecruitEntity recruit) {
        return TEXTURE[0];
    }

    public RecruitRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

}
