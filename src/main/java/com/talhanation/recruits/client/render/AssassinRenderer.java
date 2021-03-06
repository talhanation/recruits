package com.talhanation.recruits.client.render;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractOrderAbleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AssassinRenderer extends AbstractRecruitRenderer<AbstractOrderAbleEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/assassin.png"),
    };

    public AssassinRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractOrderAbleEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
