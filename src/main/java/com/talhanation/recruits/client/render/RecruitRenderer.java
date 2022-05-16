package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;

public class RecruitRenderer extends AbstractRecruitRenderer<AbstractRecruitEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public RecruitRenderer(EntityRenderDispatcher mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractRecruitEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
