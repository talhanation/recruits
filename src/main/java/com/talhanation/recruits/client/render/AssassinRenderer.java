package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractOrderAbleEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;

public class AssassinRenderer extends AbstractAssassinRenderer<AbstractOrderAbleEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/assassin.png"),
    };

    public AssassinRenderer(EntityRenderDispatcher mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractOrderAbleEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
