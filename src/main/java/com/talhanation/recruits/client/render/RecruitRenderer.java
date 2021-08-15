package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class RecruitRenderer extends AbstractManRenderer<RecruitEntity>{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public RecruitRenderer(EntityRendererManager mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(RecruitEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
