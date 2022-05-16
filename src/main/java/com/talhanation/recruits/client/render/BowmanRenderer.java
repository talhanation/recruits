package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;


public class BowmanRenderer extends AbstractRecruitRenderer<BowmanEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public BowmanRenderer(EntityRenderDispatcher mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(BowmanEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
