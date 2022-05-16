package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;


public class CrossBowmanRenderer extends AbstractRecruitRenderer<CrossBowmanEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public CrossBowmanRenderer(EntityRenderDispatcher mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(CrossBowmanEntity entity) {
        return TEXTURE[0];
    }
}
