package com.talhanation.recruits.client.render.human;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitHumanRenderer;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BowmanHumanRenderer extends AbstractRecruitHumanRenderer<BowmanEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public BowmanHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(BowmanEntity p_110775_1_) {
        return TEXTURE[0];
    }
}